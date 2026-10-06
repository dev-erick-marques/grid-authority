# Security Considerations

> This document catalogs known security shortcuts made intentionally for hackathon purposes.
> Each item describes the current behavior, the risk it introduces, and what a production
> implementation should do instead.

---

## 1. MQTT TLS — `TrustAllSslFactory`

**Location:** `device/src/main/java/com/gridauthority/device/infrastructure/mqtt/TrustAllSslFactory.java`

**Current behavior:** The device connects to the HiveMQ broker over TLS (`ssl://hivemq:8883`) but uses a
custom `X509TrustManager` that accepts any certificate without validation — including self-signed, expired,
or attacker-controlled ones.

**Risk:** Susceptible to man-in-the-middle attacks. An attacker positioned between the device and the broker
could intercept or inject MQTT messages. TLS provides transport encryption, but without certificate validation
it provides no authentication of the broker's identity.

**Production fix:** Remove `TrustAllSslFactory` and load a proper `TrustStore` containing the CA that signed
the broker's certificate. If using a public CA, the JVM's default trust store is sufficient. If using an
internal CA, distribute the CA certificate and configure it explicitly via `TrustManagerFactory`.

---

## 2. HiveMQ Broker — Self-Signed Certificate with Hardcoded Password

**Location:** `mqtt/config/config.xml`, `mqtt/Dockerfile`

**Current behavior:** The broker uses a self-signed certificate stored in a JKS keystore. The keystore and
private-key passwords are hardcoded in `config.xml`.

**Risk:** The private key is trivially accessible to anyone who reads the config file or image layers. A
compromised private key allows an attacker to impersonate the broker, defeating TLS entirely.

**Production fix:** Replace the self-signed certificate with one issued by a trusted CA (internal or public).
Inject keystore credentials at runtime via environment variables or a secrets manager (AWS Secrets Manager,
HashiCorp Vault) — never bake them into the image or config file.

---

## 3. HiveMQ Broker — No Client Authentication

**Location:** `mqtt/config/config.xml`

**Current behavior:** The broker is configured with `client-authentication-mode: NONE`, meaning any client
that can reach port `8883` can connect and publish or subscribe to any topic without credentials.

**Risk:** An unauthorized process on the same network could subscribe to command topics and observe all
control traffic, or publish arbitrary payloads to device topics.

**Production fix:** Enable one of the following authentication mechanisms:

- **Username/password authentication** via the HiveMQ Access Control extension, with per-client credentials
  and topic-level ACLs.
- **Mutual TLS (mTLS):** Require client certificates and provision each service (coordinator, devices) with
  its own. This is the preferred approach for machine-to-machine IoT scenarios, as it ties client
  authentication to the same PKI already used for transport security.

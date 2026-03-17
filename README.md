# GridAuthority

**Autonomous voltage authority for sensitive electrical grid devices — stability-driven shutdown,
cryptographically signed, permanently auditable.**

---

## Table of Contents

- [Why It Exists](#why-it-exists)
- [The Three Pillars](#the-three-pillars)
- [Running Locally](#running-locally)
- [Environment Variables](#environment-variables)
- [Frontend Monitor](#frontend-monitor)
- [API Reference](#api-reference)
- [Future Work](#future-work)

---

---

## Why It Exists

Electrical grids are sensitive. A voltage instability that crosses the **10% Coefficient of
Variation threshold** — the EN 50160 European standard for acceptable grid fluctuation — can
damage connected equipment, trigger cascading failures, or compromise the safety of the
infrastructure itself. Human reaction time is not fast enough, and rule-based scripts are not
trustworthy enough.

GridAuthority is an **autonomous control authority** that monitors device voltage in real time,
computes **[statistical stability](./docs/metrics.md)** continuously, and acts without human intervention:

- When CV exceeds 10%, it issues a `SHUTDOWN` command immediately
- After a configurable number of consecutive stable cycles, it issues a `RESTART`
- Every decision is signed by a hardware-backed AWS KMS key and anchored immutably to Hedera HCS

It ships with a real-time monitor, but that is not what it is. It is a deterministic control authority —
the only entity on the network with the cryptographic standing to alter the operational state of a device.

### Beyond shutdown — grid/generator switching with zero downtime (roadmap)

Shutdown is the last resort. In environments where downtime is not acceptable — data centers,
hospital infrastructure, industrial control systems — the same command model applies to a
different action: **switching the power source**.

When grid voltage instability is detected, GridAuthority can issue signed commands like SWITCH_TO_GENERATOR, 
SWITCH_TO_UPS, or SWITCH_TO_BACKUP upon CV threshold breach.

- Transfer switch acts in milliseconds with cryptographic assurance
- Audit trail identical: CV trigger, timestamp, signature, anchored to HCS
- Restoration via SWITCH_TO_GRID after stability returns

No core changes required — command type is a payload field. 
This extension relies on reliable device liveness detection (see Phi Accrual below) to confirm the target before switching..
 
---
## Latency & Execution Model

GridAuthority implements a hybrid architecture to balance industrial safety (speed) with governance (transparency):

Real-Time Execution (Fire-and-Forget): Critical commands (SHUTDOWN) are executed by the device in milliseconds
upon receipt. The device validates the KMS signature locally. It does not wait for Hedera consensus to act,
ensuring equipment is protected before damage occurs.

Asynchronous Anchoring: While the device acts immediately, the Coordinator sends the event to Hedera HCS in parallel.
This creates a permanent, non-repudiable audit trail of why and when the device was shut down.

Consensus-Locked Rotation: Unlike shutdown commands, Key Rotations are governed by Hedera's consensus timestamp.
Devices only transition to a new key after the consensus window is reached, preventing "split-brain" scenarios caused
by local clock drift.

## Key Management & Rotation

The project implements a secure KMS key rotation mechanism integrated with the Hedera Consensus Service (HCS). This ensures that all devices in the grid synchronize state and cryptographic keys without relying on local clocks.

### Rotation Protocol Highlights:
- **Alias-based Rotation**: Seamless transition by reassigning KMS aliases.
- **Consensus-based Activation**: Uses Hedera's network consensus timestamp to prevent issues with clock drift.
- **Grace Period**: Implementation of an `activateWindow` (e.g., 10,000ms) to ensure all devices receive the update before the old key is decommissioned.
- **Flow Control**: The Coordinator pauses command emissions during the rotation window (`now + timestamp * 1.1`).

[Detailed Rotation Specification](./docs/key-rotation.md)

## The Three Pillars

### 1. AWS KMS — Command Integrity

Every `SHUTDOWN` and `RESTART` command is signed by an ECDSA P-256 private key that **never
leaves AWS KMS**. The coordinator produces a `SignedCommandPayload` containing the canonical JSON
of the command, its ECDSA signature, and the KMS key ID. The device verifies the signature
locally using `java.security` before accepting any state change.

- Canonical JSON is produced via Jackson's `SORT_PROPERTIES_ALPHABETICALLY` — ensuring the
  `payloadHash` anchored to HCS always matches what KMS signed
- The `keyId` is carried in every payload so the device can assert it is verifying against the
  expected key, not just any key
- A command without a valid signature is rejected at the device boundary with `403 Forbidden`


### 2. Hedera HCS — Dynamic Identity & Rotation

Identity Anchoring: On startup, the AuthorityKeyPublisher fetches the public key from
KMS and publishes an  event to HCS. The device's trust anchor is the immutable ledger, 
not a vulnerable HTTP endpoint.

Seamless Transition: Key rotation is handled dynamically. The new public key metadata
is broadcasted via HCS, and devices update their local cache based on the consensus window,
requiring no manual restarts or firmware updates.


### 3. Hedera HCS — Auditability

Every governance event is anchored asynchronously to HCS. Each topic corresponds to a specific event type:

- AUTHORITY_KEY_PUBLISHED — published to the publicKeyTopicId topic; contains the coordinator's public key 
information and metadata required for devices to verify signatures.

- DECISION — published to the decisionTopicId topic; contains device identifiers, action type (e.g., SHUTDOWN, RESTART), 
stability metrics, the command payload hash, and the KMS signature.

- SURGE — published to the surgeTopicId topic; represents a simulated voltage surge event, including affected 
device identifiers, the simulated action, payload hash, and signature.

The payloadHash is always the SHA-256 of the canonical JSON signed by KMS. External auditors can verify that the anchored 
hash matches the signature without needing access to the system itself. The exact fields carried in each event may evolve 
over time as the system expands, but the anchoring and verification model remains consistent.

> The transport and broker layers have known shortcuts made for hackathon scope.
See [Security Considerations](./docs/security-considerations.md) for details and production fixes
---


## Running Locally

### Prerequisites

- Docker
- Docker Compose

### 1. Clone the repository

```bash
git clone https://github.com/dev-erick-marques/grid-authority.git
cd grid-authority
```

### 2. Prepare environment variables

Copy the example environment file and edit it with your credentials:

Linux
```bash
cp .env.example .env
```

windows
```bash
copy .env.example .env
```

Edit `.env` and configure:

- AWS KMS key
- Hedera account credentials
- HCS topic IDs

### 3. Build and start the stack

All services are built automatically inside Docker.

```bash
docker compose up --build
```
This command:

- Builds the coordinator (Spring Boot)
- Builds the device simulators (Spring Boot)
- Builds the frontend (Node → Nginx static bundle)
- Starts the MQTT broker (HiveMQ)
- Starts the full distributed stack


After startup, open:
```
http://localhost:3000
```


## Environment Variables

Copy `.env.example` to `.env` and fill in the values before running `docker compose up`.

### `.env.example`

```dotenv
# ──────────────────────────────────────────────
# AWS KMS
# ──────────────────────────────────────────────

# ARN or key ID of the ECC_NIST_P256 asymmetric KMS key used for command signing.
# Required by the coordinator. The device never touches AWS credentials.
KMS_KEY_ID=arn:aws:kms:us-east-1:123456789012:key/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx

# AWS credentials for the coordinator to call kms:Sign and kms:GetPublicKey.
# Use an IAM role in production; static credentials for local development only.
AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
AWS_REGION=us-east-1

# ──────────────────────────────────────────────
# Hedera Testnet
# ──────────────────────────────────────────────

# Hedera account that pays for HCS topic message submissions.
HEDERA_ACCOUNT_ID=0.0.XXXXX

# DER-encoded ED25519 private key for the above account (hex, no 0x prefix).
HEDERA_PRIVATE_KEY=302e020100300506032b657004220420...

# HCS topic for AUTHORITY_KEY_PUBLISHED events (coordinator public key bootstrap).
HEDERA_PUBLIC_KEY_TOPIC_ID=0.0.YYYYY

# HCS topic for DECISION events (SHUTDOWN / RESTART per device).
HEDERA_DECISION_TOPIC_ID=0.0.ZZZZZ

# HCS topic for SURGE simulation events.
HEDERA_SURGE_TOPIC_ID=0.0.WWWWW

```

### KMS Setup

Create an asymmetric KMS key with:

```
Key type:  Asymmetric
Key usage: Sign and verify
Key spec:  ECC_NIST_P256
```

Attach a least-privilege IAM policy to the coordinator's role or user:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "kms:Sign",
        "kms:GetPublicKey"
      ],
      "Resource": "arn:aws:kms:<region>:<account>:key/<key-id>"
    }
  ]
}
```

The coordinator calls `GetPublicKey` once at boot (to publish to HCS) and `Sign` on every
control command. No other KMS permissions are needed.

### Hedera Setup

Create three HCS topics on the [Hedera portal](https://portal.hedera.com) or via the SDK:


Create three topics and set the resulting IDs in your `.env`:
- `HEDERA_PUBLIC_KEY_TOPIC_ID` — for `AUTHORITY_KEY_PUBLISHED`
- `HEDERA_DECISION_TOPIC_ID` — for `DECISION` events
- `HEDERA_SURGE_TOPIC_ID` — for `SURGE` events

Verify on-chain activity at [HashScan Testnet](https://hashscan.io/testnet).

---

## Frontend Monitor

The React/TypeScript frontend connects to the coordinator via SSE and displays:

- **Live voltage graph** per device (Recharts) with EN 50160 threshold overlay
- **Current device state** (`ACTIVE` / `SHUTDOWN`) per device
- **CV value** in real time
- **KMS signing log** — each signed command with its `keyId` and `payloadHash`
- **HCS anchor confirmations** — consensus timestamps per anchored event

Access at `http://localhost:3000` after `docker compose up`.

---

## API Reference

### Coordinator (`localhost:8080`)

| Method | Path                          | Description                                                          |
|--------|-------------------------------|----------------------------------------------------------------------|
| `POST` | `/api/metrics`                | Ingest voltage telemetry from a device                               |
| `GET`  | `/api/metrics/stream`         | SSE stream of real-time metrics and decisions                        |
| `GET`  | `/api/policy`                 | Current policy configuration (`thresholdCv`, `stableCyclesRequired`) |
| `POST` | `/api/simulation/surge/start` | Start voltage surge simulation                                       |
| `POST` | `/api/simulation/surge/stop`  | Stop voltage surge simulation                                        |
| `GET`  | `/actuator/health`            | Spring Boot health endpoint                                          |

### Device (`localhost:8081`)

| Method | Path               | Description                                   |
|--------|--------------------|-----------------------------------------------|
| `POST` | `/api/commands`    | Receive a signed command from the coordinator |
| `GET`  | `/actuator/health` | Spring Boot health endpoint                   |

---

## Future Work

### Phi Accrual Failure Detection

The current stability model* evaluates voltage CV over a sliding window but does not actively detect unresponsive devices. 
A device that stops sending telemetry is currently ignored, which can delay corrective actions beyond simple shutdown.
Phi Accrual — the failure detector used in production by Apache Cassandra and Akka — introduces a continuous suspicion
metric φ, derived from historical heartbeat intervals for each device. This allows the coordinator to make proactive, intelligent
decisions instead of only triggering SHUTDOWN:

- φ ≥ 8.0 corresponds to ~99.99% confidence that the device is unresponsive

- Instead of only issuing SHUTDOWN, the coordinator can trigger contextual actions, such as switching to a backup power source
or rerouting load

- φ rising across multiple devices provides an early indicator of systemic issues and can guide global coordinator 
actions beyond single-device shutdowns

This approach enables GridAuthority to implement autonomous, stability-driven interventions that maintain operational 
continuity — aligning with the “Beyond shutdown” philosophy introduced earlier.

### Coordinator SPOF Mitigation

The coordinator is a single point of failure. Hedera HCS is a natural fit as the consensus
mechanism for leader election in a clustered deployment: the winning coordinator publishes its
`AUTHORITY_KEY_PUBLISHED` event and all others stand down. The device trust derivation model is
unchanged.

### Device Inventory Anchoring

A `DEVICE_REGISTERED` event per device on first telemetry receipt would produce an auditable
inventory snapshot on HCS — enabling external verification of which devices were active across
coordinator lifecycle boundaries.

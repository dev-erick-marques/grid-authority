package com.gridauthority.coordinator.signing;

import com.gridauthority.coordinator.application.dto.SignedCommandPayload;
import com.gridauthority.coordinator.domain.model.DeviceCommand;
import com.gridauthority.coordinator.infrastructure.config.AuthoritySigningProperties;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class LocalSigningService {
    private final AuthoritySigningProperties props;
    private final AtomicReference<KeyPair> keyPair = new AtomicReference<>();

    public LocalSigningService(AuthoritySigningProperties p) {
        props = p;
        init();
    }

    private void init() {
        try {
            if (props.getPrivateKeyBase64() != null && !props.getPrivateKeyBase64().isBlank()) {
                byte[] der = Base64.getDecoder().decode(props.getPrivateKeyBase64());
                PrivateKey pk = KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(der));
                String pub = System.getenv("AUTHORITY_PUBLIC_KEY_BASE64");
                if (pub == null || pub.isBlank())
                    throw new IllegalStateException("AUTHORITY_PUBLIC_KEY_BASE64 is required with AUTHORITY_PRIVATE_KEY_BASE64");
                PublicKey publicKey = KeyFactory.getInstance("EC").generatePublic(new java.security.spec.X509EncodedKeySpec(Base64.getDecoder().decode(pub)));
                keyPair.set(new KeyPair(publicKey, pk));
            } else {
                KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
                g.initialize(256);
                keyPair.set(g.generateKeyPair());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to initialize local authority key", e);
        }
    }

    public SignedCommandPayload sign(String deviceId, DeviceCommand action) {
        return signAction(deviceId, action.name());
    }

    public SignedCommandPayload signAction(String deviceId, String action) {
        try {
            String canonical = String.format(Locale.ROOT, "{\"action\":\"%s\",\"commandId\":\"%s\",\"deviceId\":\"%s\",\"issuedAt\":%d}", action, UUID.randomUUID(), deviceId, System.currentTimeMillis());
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initSign(keyPair.get().getPrivate());
            s.update(canonical.getBytes(StandardCharsets.UTF_8));
            return new SignedCommandPayload(Base64.getEncoder().encodeToString(s.sign()), canonical);
        } catch (Exception e) {
            throw new IllegalStateException("Local command signing failed", e);
        }
    }

    public String publicKeyBase64() {
        return Base64.getEncoder().encodeToString(keyPair.get().getPublic().getEncoded());
    }

    public String keyId() {
        return props.getKeyId();
    }
}

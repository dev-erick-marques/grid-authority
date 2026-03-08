package com.gridauthority.device.aplication.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gridauthority.device.infrastructure.config.CoordinatorProperties;
import com.gridauthority.device.infrastructure.config.SignatureVerificationProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;


@Slf4j
@Service
@RequiredArgsConstructor
public class CommandVerificationService {

    private final CoordinatorProperties coordinatorProperties;
    private final SignatureVerificationProperties verificationProperties;
    private final RestClient restClient;

    private PublicKey coordinatorPublicKey;

    public record PublicKeyResponseDTO(
            @JsonProperty("keyId") String keyId,
            @JsonProperty("signingAlgorithm") String signingAlgorithm,
            @JsonProperty("publicKeyBase64") String publicKeyBase64
    ) {}

    @PostConstruct
    public void loadPublicKey() {
        if (verificationDisabled()) {
            log.warn("[VERIFY] Signature verification DISABLED — accepting all commands. Enable in production.");
            return;
        }

        try {
            PublicKeyResponseDTO response = fetchCoordinatorPublicKey();
            byte[] derBytes = decodePublicKey(response);
            coordinatorPublicKey = buildEcPublicKey(derBytes);

            log.info("[VERIFY] Coordinator EC public key loaded — keyId={} algorithm={} bytes={}",
                    response.keyId(), response.signingAlgorithm(), derBytes.length);

        } catch (Exception e) {
            log.error("[VERIFY] Failed to load public key from coordinator: {}", e.getMessage());
            throw new IllegalStateException("Cannot start without coordinator public key", e);
        }
    }

    public boolean verify(String signatureBase64, String canonicalJson, long issuedAt) {

        if (verificationDisabled()) {
            return true;
        }

        return publicKeyLoaded()
                && timestampValid(issuedAt)
                && !isDevMarker(signatureBase64)
                && verifySignature(signatureBase64, canonicalJson);
    }

    private boolean verificationDisabled() {
        return !verificationProperties.isEnabled();
    }

    private boolean publicKeyLoaded() {
        if (coordinatorPublicKey == null) {
            log.error("[VERIFY] Public key not loaded — rejecting command");
            return false;
        }
        return true;
    }

    private boolean timestampValid(long issuedAt) {
        long age = System.currentTimeMillis() - issuedAt;

        if (age > verificationProperties.getTimestampToleranceMs() || age < 0) {
            log.warn("[VERIFY] Command rejected — timestamp age={}ms outside tolerance={}ms",
                    age, verificationProperties.getTimestampToleranceMs());
            return false;
        }

        return true;
    }

    private boolean isDevMarker(String signatureBase64) {
        if ("NO_SIGNATURE".equals(signatureBase64)) {
            log.warn("[VERIFY] Received unsigned command (dev mode marker) — rejecting");
            return true;
        }
        return false;
    }

    private boolean verifySignature(String signatureBase64, String canonicalJson) {
        try {
            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
            byte[] messageBytes = canonicalJson.getBytes(StandardCharsets.UTF_8);

            Signature sig = Signature.getInstance("SHA256withECDSA");
            sig.initVerify(coordinatorPublicKey);
            sig.update(messageBytes);

            boolean valid = sig.verify(signatureBytes);

            if (!valid) {
                log.warn("[VERIFY] Invalid ECDSA signature for canonicalJson={}", canonicalJson);
            }

            return valid;

        } catch (Exception e) {
            log.error("[VERIFY] Signature verification error: {}", e.getMessage());
            return false;
        }
    }

    private PublicKeyResponseDTO fetchCoordinatorPublicKey() {
        String url = coordinatorProperties.getUrl() + "/api/authority/public-key";

        PublicKeyResponseDTO response = restClient.get()
                .uri(url)
                .retrieve()
                .body(PublicKeyResponseDTO.class);

        if (response == null || response.publicKeyBase64() == null || response.publicKeyBase64().isBlank()) {
            throw new IllegalStateException("Coordinator returned empty public key");
        }

        return response;
    }

    private byte[] decodePublicKey(PublicKeyResponseDTO response) {
        return Base64.getDecoder().decode(response.publicKeyBase64());
    }

    private PublicKey buildEcPublicKey(byte[] derBytes) throws Exception {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(derBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        return keyFactory.generatePublic(keySpec);
    }
}
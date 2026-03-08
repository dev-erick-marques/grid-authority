package com.gridauthority.device.aplication.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gridauthority.device.domain.exception.CoordinatorPublicKeyUnavailableException;
import com.gridauthority.device.domain.exception.InvalidPublicKeyFormatException;
import com.gridauthority.device.domain.exception.SignatureVerificationException;
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
import java.security.SignatureException;
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

        PublicKeyResponseDTO response = fetchCoordinatorPublicKey();
        byte[] derBytes = decodePublicKey(response);
        coordinatorPublicKey = buildEcPublicKey(derBytes);

        log.info("[VERIFY] Coordinator EC public key loaded — keyId={} algorithm={} bytes={}",
                response.keyId(), response.signingAlgorithm(), derBytes.length);
    }

    public void verify(String signatureBase64, String canonicalJson, long issuedAt) {
        if (verificationDisabled()) {
            return;
        }

        if (!publicKeyLoaded()) {
            throw new SignatureVerificationException("Public key not loaded — cannot verify command");
        }
        if (!timestampValid(issuedAt)) {
            throw new SignatureVerificationException("Command rejected — timestamp outside tolerance");
        }
        if (isDevMarker(signatureBase64)) {
            throw new SignatureVerificationException("Unsigned command (dev marker) rejected");
        }
        if (!verifySignature(signatureBase64, canonicalJson)) {
            throw new SignatureVerificationException("Invalid ECDSA signature");
        }
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

        } catch (SignatureException e) {
            log.warn("[VERIFY] Malformed signature bytes: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            throw new SignatureVerificationException(
                    "Cryptographic error during signature verification: " + e.getMessage(), e);
        }
    }

    private PublicKeyResponseDTO fetchCoordinatorPublicKey() {
        String url = coordinatorProperties.getUrl() + "/api/authority/public-key";
        try {
            PublicKeyResponseDTO response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(PublicKeyResponseDTO.class);

            if (response == null || response.publicKeyBase64() == null || response.publicKeyBase64().isBlank()) {
                throw new CoordinatorPublicKeyUnavailableException(
                        "Coordinator returned empty or null public key from url=" + url);
            }
            return response;
        } catch (CoordinatorPublicKeyUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new CoordinatorPublicKeyUnavailableException(
                    "Failed to reach coordinator at url=" + url, e);
        }
    }

    private byte[] decodePublicKey(PublicKeyResponseDTO response) {
        try {
            return Base64.getDecoder().decode(response.publicKeyBase64());
        } catch (IllegalArgumentException e) {
            throw new InvalidPublicKeyFormatException(
                    "Coordinator public key is not valid Base64: " + e.getMessage(), e);
        }
    }

    private PublicKey buildEcPublicKey(byte[] derBytes) {
        try {
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(derBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            return keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            throw new InvalidPublicKeyFormatException(
                    "Coordinator public key is not a valid EC DER-encoded key: " + e.getMessage(), e);
        }
    }
}
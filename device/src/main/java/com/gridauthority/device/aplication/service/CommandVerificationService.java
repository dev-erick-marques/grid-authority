package com.gridauthority.device.aplication.service;

import com.gridauthority.device.aplication.dto.CommandSigningContext;
import com.gridauthority.device.domain.exception.SignatureVerificationException;
import com.gridauthority.device.infrastructure.config.SignatureVerificationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommandVerificationService {

    private final HcsKeyResolver hcsKeyResolver;
    private final SignatureVerificationProperties verificationProperties;
    private final ObjectMapper objectMapper;

    public CommandSigningContext verify(String signatureBase64, String canonicalJson) {
        PublicKey publicKey = hcsKeyResolver.getResolvedPublicKey();

        if (publicKey == null) {
            throw new SignatureVerificationException(
                    "Public key not resolved from HCS — cannot verify command. " +
                            "Check hcs.public-key-topic-id.");
        }

        verifySignature(signatureBase64, canonicalJson, publicKey);

        CommandSigningContext context = deserializeContext(canonicalJson);

        if (!timestampValid(context.issuedAt())) {
            throw new SignatureVerificationException(
                    "Command rejected — timestamp outside tolerance");
        }

        return context;
    }

    private CommandSigningContext deserializeContext(String canonicalJson) {
        try {
            return objectMapper.readValue(canonicalJson, CommandSigningContext.class);
        } catch (Exception e) {
            throw new SignatureVerificationException(
                    "Command rejected — canonicalJson could not be deserialized: " + e.getMessage(), e);
        }
    }

    private boolean timestampValid(long issuedAt) {
        long age = System.currentTimeMillis() - issuedAt;
        if (age > verificationProperties.getTimestampToleranceMs() || age < 0) {
            log.warn("[VERIFY] Command rejected — timestamp age={}ms tolerance={}ms",
                    age, verificationProperties.getTimestampToleranceMs());
            return false;
        }
        return true;
    }

    private void verifySignature(String signatureBase64, String canonicalJson,
                                 PublicKey publicKey) {
        try {
            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
            byte[] messageBytes   = canonicalJson.getBytes(StandardCharsets.UTF_8);

            Signature sig = Signature.getInstance("SHA256withECDSA");
            sig.initVerify(publicKey);
            sig.update(messageBytes);

            if (!sig.verify(signatureBytes)) {
                log.warn("[VERIFY] Invalid ECDSA signature for canonicalJson={}", canonicalJson);
                throw new SignatureVerificationException("Invalid ECDSA signature");
            }

        } catch (SignatureException e) {
            log.warn("[VERIFY] Malformed signature bytes: {}", e.getMessage());
            throw new SignatureVerificationException(
                    "Malformed signature bytes: " + e.getMessage(), e);
        } catch (SignatureVerificationException e) {
            throw e;
        } catch (Exception e) {
            throw new SignatureVerificationException(
                    "Cryptographic error during verification: " + e.getMessage(), e);
        }
    }
}
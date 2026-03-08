package com.gridauthority.coordinator.infrastructure.kms;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.gridauthority.coordinator.application.dto.CommandSigningContext;
import com.gridauthority.coordinator.application.dto.SignedCommandPayload;
import com.gridauthority.coordinator.infrastructure.config.KmsProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.GetPublicKeyRequest;
import software.amazon.awssdk.services.kms.model.GetPublicKeyResponse;
import software.amazon.awssdk.services.kms.model.MessageType;
import software.amazon.awssdk.services.kms.model.SignRequest;
import software.amazon.awssdk.services.kms.model.SigningAlgorithmSpec;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class KmsSigningService {

    private final KmsClient kmsClient;
    private final KmsProperties kmsProperties;
    private final CanonicalJsonMapper canonicalJsonMapper;
    private byte[] cachedPublicKeyDer;

    @PostConstruct
    public void loadPublicKey() {
        if (!kmsProperties.isEnabled()) {
            log.warn("[KMS] Disabled — commands will NOT be signed. Set kms.enabled=true in production.");
            return;
        }
        cachedPublicKeyDer = fetchPublicKeyDerFromKms();
        log.info("[KMS] Public key loaded — keyId={} algorithm={}",
                kmsProperties.getKeyId(), kmsProperties.getSigningAlgorithm());
    }

    private byte[] fetchPublicKeyDerFromKms() {
        try {
            return kmsClient.getPublicKey(
                    GetPublicKeyRequest.builder()
                            .keyId(kmsProperties.getKeyId())
                            .build()
            ).publicKey().asByteArray();
        } catch (Exception e) {
            log.error("[KMS] Failed to load public key on startup — keyId={}: {}",
                    kmsProperties.getKeyId(), e.getMessage());
            throw new IllegalStateException("KMS public key unavailable on startup", e);
        }
    }

    public SignedCommandPayload sign(String deviceId, String action) {
        long issuedAt = System.currentTimeMillis();
        CommandSigningContext context = new CommandSigningContext(action, deviceId, issuedAt);
        String canonicalJson = canonicalJsonMapper.writeCanonicalAsString(context);

        if (!kmsProperties.isEnabled()) {
            return buildUnsignedPayload(deviceId, action, issuedAt, canonicalJson);
        }

        String signatureBase64 = signWithKms(context);
        log.info("[KMS] Signed action={} device={} keyId={} canonical={}",
                action, deviceId, kmsProperties.getKeyId(), canonicalJson);

        return new SignedCommandPayload(
                deviceId, action, issuedAt,
                kmsProperties.getKeyId(), kmsProperties.getSigningAlgorithm(),
                signatureBase64, canonicalJson
        );
    }

    private String signWithKms(CommandSigningContext context) {
        byte[] canonicalBytes = canonicalJsonMapper.writeCanonical(context);
        byte[] signatureBytes = kmsClient.sign(SignRequest.builder()
                .keyId(kmsProperties.getKeyId())
                .message(SdkBytes.fromByteArray(canonicalBytes))
                .messageType(MessageType.RAW)
                .signingAlgorithm(SigningAlgorithmSpec.fromValue(kmsProperties.getSigningAlgorithm()))
                .build()
        ).signature().asByteArray();
        return Base64.getEncoder().encodeToString(signatureBytes);
    }

    private SignedCommandPayload buildUnsignedPayload(
            String deviceId, String action, long issuedAt, String canonicalJson) {
        return new SignedCommandPayload(
                deviceId, action, issuedAt,
                "LOCAL_DEV", kmsProperties.getSigningAlgorithm(),
                "NO_SIGNATURE", canonicalJson
        );
    }

    public PublicKeyResponseDTO getPublicKeyResponse() {
        if (cachedPublicKeyDer == null) {
            throw new IllegalStateException("KMS public key not loaded");
        }
        return new PublicKeyResponseDTO(
                kmsProperties.getKeyId(),
                kmsProperties.getSigningAlgorithm(),
                Base64.getEncoder().encodeToString(cachedPublicKeyDer)
        );
    }

    public record PublicKeyResponseDTO(
            @JsonProperty("keyId") String keyId,
            @JsonProperty("signingAlgorithm") String signingAlgorithm,
            @JsonProperty("publicKeyBase64") String publicKeyBase64
    ) {}
}
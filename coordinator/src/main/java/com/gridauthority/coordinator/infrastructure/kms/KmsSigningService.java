package com.gridauthority.coordinator.infrastructure.kms;

import com.gridauthority.coordinator.application.dto.PublicKeyResponseDTO;
import com.gridauthority.coordinator.application.dto.SignedCommandPayload;
import com.gridauthority.coordinator.domain.exceptions.KmsPublicKeyNotLoadedException;
import com.gridauthority.coordinator.domain.exceptions.KmsSigningFailedException;
import com.gridauthority.coordinator.domain.exceptions.KmsUnavailableException;
import com.gridauthority.coordinator.infrastructure.config.KmsProperties;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.GetPublicKeyRequest;
import software.amazon.awssdk.services.kms.model.MessageType;
import software.amazon.awssdk.services.kms.model.SignRequest;
import software.amazon.awssdk.services.kms.model.SigningAlgorithmSpec;

import java.util.Arrays;
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
        cachedPublicKeyDer = fetchPublicKeyDerFromKms();
        log.info("[KMS] Public key loaded keyId={} algorithm={}",
                kmsProperties.getKeyId(), kmsProperties.getSigningAlgorithm());
    }

    public boolean reloadPublicKey() {
        byte[] freshDer = fetchPublicKeyDerFromKms();
        if (Arrays.equals(freshDer, cachedPublicKeyDer)) {
            log.debug("[KMS] Public key unchanged keyId={}", kmsProperties.getKeyId());
            return false;
        }
        cachedPublicKeyDer = freshDer;
        log.info("[KMS] Public key rotated keyId={} algorithm={}",
                kmsProperties.getKeyId(), kmsProperties.getSigningAlgorithm());
        return true;
    }

    public SigningResult issueCommand(String deviceId, String action) {
        long issuedAt = System.currentTimeMillis();
        CommandSigningContext context = new CommandSigningContext(action, deviceId, issuedAt);
        String canonicalJson = canonicalJsonMapper.writeCanonicalAsString(context);
        String signatureBase64 = signCanonical(context);

        log.debug("[KMS] Signed action={} device={} keyId={}",
                action, deviceId, shortKeyId(kmsProperties.getKeyId()));

        SignedCommandPayload payload = new SignedCommandPayload(signatureBase64, canonicalJson);
        return new SigningResult(payload, context, kmsProperties.getKeyId(), kmsProperties.getSigningAlgorithm());
    }

    private byte[] fetchPublicKeyDerFromKms() {
        try {
            return kmsClient.getPublicKey(
                    GetPublicKeyRequest.builder()
                            .keyId(kmsProperties.getKeyId())
                            .build()
            ).publicKey().asByteArray();
        } catch (Exception e) {
            log.error("[KMS] Failed to load public key keyId={}: {}",
                    kmsProperties.getKeyId(), e.getMessage());
            throw new KmsUnavailableException(
                    "KMS public key unavailable for keyId=" + kmsProperties.getKeyId(), e);
        }
    }

    private String signCanonical(CommandSigningContext context) {
        byte[] canonicalBytes = canonicalJsonMapper.writeCanonical(context);
        try {
            byte[] signatureBytes = kmsClient.sign(SignRequest.builder()
                    .keyId(kmsProperties.getKeyId())
                    .message(SdkBytes.fromByteArray(canonicalBytes))
                    .messageType(MessageType.RAW)
                    .signingAlgorithm(SigningAlgorithmSpec.fromValue(kmsProperties.getSigningAlgorithm()))
                    .build()
            ).signature().asByteArray();
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (Exception e) {
            throw new KmsSigningFailedException(
                    "KMS signing failed for keyId=" + kmsProperties.getKeyId()
                            + " action=" + context.action()
                            + " device=" + context.deviceId(), e);
        }
    }

    public PublicKeyResponseDTO getPublicKeyResponse() {
        if (cachedPublicKeyDer == null) {
            throw new KmsPublicKeyNotLoadedException(
                    "KMS public key not loaded — startup failed");
        }
        return new PublicKeyResponseDTO(
                kmsProperties.getKeyId(),
                kmsProperties.getSigningAlgorithm(),
                Base64.getEncoder().encodeToString(cachedPublicKeyDer)
        );
    }

    public String getKeyId() {
        return kmsProperties.getKeyId();
    }

    private static String shortKeyId(String keyId) {
        if (keyId == null) return "none";
        int slash = keyId.lastIndexOf('/');
        return slash >= 0 ? keyId.substring(slash + 1) : keyId;
    }
}
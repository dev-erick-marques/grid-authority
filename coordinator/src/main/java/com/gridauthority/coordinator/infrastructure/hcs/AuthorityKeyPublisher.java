package com.gridauthority.coordinator.infrastructure.hcs;

import com.gridauthority.coordinator.infrastructure.kms.KmsSigningService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;


@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorityKeyPublisher {

    private final KmsSigningService kmsSigningService;
    private final HcsAnchorService hcsAnchorService;

    @PostConstruct
    public void publishOnBoot() {
        try {
            KmsSigningService.PublicKeyResponseDTO keyResponse =
                    kmsSigningService.getPublicKeyResponse();

            String payloadHash = sha256(keyResponse.publicKeyBase64());

            HcsEvent event = HcsEvent.authorityKeyPublished(
                    keyResponse.keyId(),
                    keyResponse.signingAlgorithm(),
                    keyResponse.publicKeyBase64(),
                    payloadHash
            );

            hcsAnchorService.anchorAuthorityKey(event);

            log.info("[HCS] AUTHORITY_KEY_PUBLISHED — keyId={} payloadHash={}",
                    keyResponse.keyId(), payloadHash);

        } catch (Exception e) {
            log.error("[HCS] Failed to publish authority key on boot: {}", e.getMessage());
        }
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return "HASH_UNAVAILABLE";
        }
    }
}
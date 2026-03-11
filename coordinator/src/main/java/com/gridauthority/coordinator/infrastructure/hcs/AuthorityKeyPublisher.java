package com.gridauthority.coordinator.infrastructure.hcs;

import com.gridauthority.coordinator.infrastructure.kms.KmsSigningService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorityKeyPublisher {

    private final KmsSigningService kmsSigningService;
    private final HcsAnchorService hcsAnchorService;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void publishOnBoot() {
        try {
            KmsSigningService.PublicKeyResponseDTO keyResponse =
                    kmsSigningService.getPublicKeyResponse();

            HcsPayload payload = HcsPayload.builder()
                    .eventType(HcsEvent.EventType.AUTHORITY_KEY_PUBLISHED_ON_BOOT.name())
                    .keyId(keyResponse.keyId())
                    .signingAlgorithm(keyResponse.signingAlgorithm())
                    .publicKeyBase64(keyResponse.publicKeyBase64())
                    .timestamp(System.currentTimeMillis())
                    .build();

            HcsEvent event = HcsEvent.of(payload, objectMapper);

            hcsAnchorService.anchorAuthorityKey(event);

            log.info("[HCS] AUTHORITY_KEY_PUBLISHED — keyId={} sha256={}",
                    keyResponse.keyId(), event.sha256());

        } catch (Exception e) {
            log.error("[HCS] Failed to publish authority key on boot: {}", e.getMessage());
        }
    }
}
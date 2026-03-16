package com.gridauthority.coordinator.infrastructure.hcs;

import com.gridauthority.coordinator.application.dto.PublicKeyResponseDTO;
import com.gridauthority.coordinator.infrastructure.kms.KmsSigningService;
import com.gridauthority.coordinator.infrastructure.registry.DeviceRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorityKeyPublisher {

    private final KmsSigningService        kmsSigningService;
    private final HcsAnchorService         hcsAnchorService;
    private final ObjectMapper             objectMapper;

    @PostConstruct
    public void publishOnBoot() {
        publish(HcsEvent.EventType.AUTHORITY_KEY_PUBLISHED_ON_BOOT);
    }

    private void publish(HcsEvent.EventType eventType) {
        try {
            PublicKeyResponseDTO keyResponse = kmsSigningService.getPublicKeyResponse();

            HcsPayload payload = HcsPayload.builder()
                    .eventType(eventType.name())
                    .keyId(keyResponse.keyId())
                    .signingAlgorithm(keyResponse.signingAlgorithm())
                    .publicKeyBase64(keyResponse.publicKeyBase64())
                    .timestamp(System.currentTimeMillis())
                    .build();

            HcsEvent event = HcsEvent.of(payload, objectMapper);
            hcsAnchorService.anchorAuthorityKey(event);

            log.info("[HCS] {} — keyId={} sha256={}",
                    eventType.name(), keyResponse.keyId(), event.sha256());

        } catch (Exception e) {
            log.error("[HCS] Failed to publish authority key eventType={}: {}",
                    eventType.name(), e.getMessage());
        }
    }
}
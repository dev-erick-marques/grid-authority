package com.gridauthority.coordinator.infrastructure.hcs;

import com.gridauthority.coordinator.application.dto.PublicKeyResponseDTO;
import com.gridauthority.coordinator.infrastructure.config.HcsProperties;
import com.gridauthority.coordinator.infrastructure.kms.KmsSigningService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorityKeyPublisher {

    private static final double COORDINATOR_WINDOW_MULTIPLIER = 1.1;

    private final KmsSigningService kmsSigningService;
    private final HcsAnchorService hcsAnchorService;
    private final HcsProperties hcsProperties;
    private final ObjectMapper objectMapper;

    private final AtomicReference<Instant> commandsAllowedAfter =
            new AtomicReference<>(Instant.MAX);

    @PostConstruct
    public void publishOnBoot() {
        publish(HcsEvent.EventType.AUTHORITY_KEY_PUBLISHED_ON_BOOT);
    }

    public void publishIfRotated() {
        if (!kmsSigningService.reloadPublicKey()) {
            log.debug("[HCS] Key unchanged — skipping HCS publication");
            return;
        }
        publish(HcsEvent.EventType.AUTHORITY_KEY_PUBLISHED_ON_ROTATION);
    }

    public boolean isKeyActive() {
        return Instant.now().isAfter(commandsAllowedAfter.get());
    }

    public Instant getCommandsAllowedAfter() {
        return commandsAllowedAfter.get();
    }

    private void publish(HcsEvent.EventType eventType) {
        try {
            PublicKeyResponseDTO keyResponse = kmsSigningService.getPublicKeyResponse();
            long now = System.currentTimeMillis();
            long windowMs = hcsProperties.getActivationWindowMs();

            HcsPayload payload = HcsPayload.builder()
                    .eventType(eventType.name())
                    .keyId(keyResponse.keyId())
                    .signingAlgorithm(keyResponse.signingAlgorithm())
                    .publicKeyBase64(keyResponse.publicKeyBase64())
                    .timestamp(now)
                    .activationWindowMs(windowMs)
                    .build();

            HcsEvent event = HcsEvent.of(payload, objectMapper);
            hcsAnchorService.anchorAuthorityKey(event);

            long coordinatorWindowMs = (long) (windowMs * COORDINATOR_WINDOW_MULTIPLIER);
            commandsAllowedAfter.set(Instant.ofEpochMilli(now + coordinatorWindowMs));

            log.info("[HCS] {} keyId={} commandsAllowedAfter={} hash={}",
                    eventType.name(), shortKeyId(keyResponse.keyId()),
                    commandsAllowedAfter.get(), shortHash(event.sha256()));

        } catch (Exception e) {
            log.error("[HCS] Failed to publish authority key eventType={}: {}",
                    eventType.name(), e.getMessage());
        }
    }

    private static String shortKeyId(String keyId) {
        if (keyId == null) return "none";
        int slash = keyId.lastIndexOf('/');
        return slash >= 0 ? keyId.substring(slash + 1) : keyId;
    }

    private static String shortHash(String hash) {
        return hash != null && hash.length() > 12 ? hash.substring(0, 12) : hash;
    }
}
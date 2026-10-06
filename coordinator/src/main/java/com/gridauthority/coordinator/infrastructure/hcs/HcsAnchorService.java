package com.gridauthority.coordinator.infrastructure.hcs;

import com.gridauthority.coordinator.infrastructure.audit.AuditEventPublisher;
import com.gridauthority.coordinator.infrastructure.audit.AuditLogEntry;
import com.gridauthority.coordinator.infrastructure.config.HcsProperties;
import com.hedera.hashgraph.sdk.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class HcsAnchorService {

    private final HcsProperties hcsProperties;
    private final ObjectMapper objectMapper;
    private Client hederaClient;
    private final AuditEventPublisher auditEventPublisher;

    @PostConstruct
    public void init() {
        try {
            hederaClient = buildClient();
            log.info("[HCS] Client initialized — network={} operator={}",
                    hcsProperties.getNetwork(), hcsProperties.getAccountId());
        } catch (Exception e) {
            throw new IllegalStateException(
                    "[HCS] Failed to initialize Hedera client: " + e.getMessage(), e);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (hederaClient != null) {
            try { hederaClient.close(); }
            catch (TimeoutException e) {
                log.warn("[HCS] Timeout closing Hedera client: {}", e.getMessage());
            }
        }
    }

    public void anchorAuthorityKey(HcsEvent event) {
        publish(hcsProperties.getPublicKeyTopicId(), event);
    }

    public void anchorDecision(HcsEvent event) {
        publish(hcsProperties.getDecisionTopicId(), event);
    }

    public void anchorSurge(HcsEvent event) {
        publish(hcsProperties.getSurgeTopicId(), event);
    }

    private void publish(String topicIdStr, HcsEvent event) {
        String eventType = event.payload().eventType();
        if (topicIdStr == null || topicIdStr.isBlank()) {
            log.error("[HCS] Cannot anchor {} — topicId not configured", eventType);
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(event);
            TopicId topicId = TopicId.fromString(topicIdStr);

            new TopicMessageSubmitTransaction()
                    .setTopicId(topicId)
                    .setMessage(payload.getBytes(StandardCharsets.UTF_8))
                    .executeAsync(hederaClient)
                    .thenRun(() -> {
                        log.info("[HCS] Anchored eventType={} topicId={} hash={}",
                                eventType, topicIdStr, shortHash(event.sha256()));
                        auditEventPublisher.publish(AuditLogEntry.fromHcsEvent(event, "HCS_ANCHORED", topicIdStr));
                    })
                    .exceptionally(e -> {
                        log.error("[HCS] Failed to anchor eventType={} topicId={} — {}",
                                eventType, topicIdStr, e.getMessage());
                        auditEventPublisher.publish(AuditLogEntry.fromHcsEvent(event, "HCS_ERROR", topicIdStr));
                        return null;
                    });

        } catch (Exception e) {
            log.error("[HCS] Failed to serialize HCS event eventType={} — {}", eventType, e.getMessage());
        }
    }

    private static String shortHash(String hash) {
        return hash != null && hash.length() > 12 ? hash.substring(0, 12) : hash;
    }

    private Client buildClient() {
        AccountId operatorId = AccountId.fromString(hcsProperties.getAccountId());
        PrivateKey operatorKey = PrivateKey.fromString(hcsProperties.getPrivateKey());

        Client client = switch (hcsProperties.getNetwork()) {
            case "mainnet"    -> Client.forMainnet();
            case "previewnet" -> Client.forPreviewnet();
            default           -> Client.forTestnet();
        };
        client.setOperator(operatorId, operatorKey);
        return client;
    }
}
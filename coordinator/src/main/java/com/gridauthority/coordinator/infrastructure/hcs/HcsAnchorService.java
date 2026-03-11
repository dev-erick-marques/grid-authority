package com.gridauthority.coordinator.infrastructure.hcs;

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

    @PostConstruct
    public void init() {
        if (!hcsProperties.isEnabled()) {
            log.warn("[HCS] Disabled — events will NOT be anchored. " +
                    "Set hcs.enabled=true in production.");
            return;
        }
        try {
            hederaClient = buildClient();
            log.info("[HCS] Client initialized — network={} operator={}",
                    hcsProperties.getNetwork(), hcsProperties.getAccountId());
        } catch (Exception e) {
            log.error("[HCS] Failed to initialize Hedera client: {}", e.getMessage());
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
        publish(hcsProperties.getPublicKeyTopicId(), event, "AUTHORITY_KEY_PUBLISHED");
    }

    public void anchorDecision(HcsEvent event) {
        publish(hcsProperties.getDecisionTopicId(), event, "DECISION");
    }

    public void anchorSurge(HcsEvent event) {
        publish(hcsProperties.getSurgeTopicId(), event, "SURGE");
    }

    private void publish(String topicIdStr, HcsEvent event, String label) {
        if (!hcsProperties.isEnabled()) return;
        if (hederaClient == null) {
            log.error("[HCS] Cannot anchor {} — client not initialized", label);
            return;
        }
        if (topicIdStr == null || topicIdStr.isBlank()) {
            log.error("[HCS] Cannot anchor {} — topicId not configured", label);
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(event);
            TopicId topicId = TopicId.fromString(topicIdStr);

            new TopicMessageSubmitTransaction()
                    .setTopicId(topicId)
                    .setMessage(payload.getBytes(StandardCharsets.UTF_8))
                    .executeAsync(hederaClient)
                    .thenRun(() -> log.info("[HCS] Anchored eventType={} topicId={} device={} payloadHash={}",
                            label, topicIdStr, event.payload().deviceId(), event.sha256()))
                    .exceptionally(e -> {
                        log.error("[HCS] Failed to anchor eventType={} topicId={} — {}",
                                label, topicIdStr, e.getMessage());
                        return null;
                    });

        } catch (Exception e) {
            log.error("[HCS] Failed to serialize HCS event eventType={} — {}", label, e.getMessage());
        }
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
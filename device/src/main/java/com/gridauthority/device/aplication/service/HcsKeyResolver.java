package com.gridauthority.device.aplication.service;

import com.gridauthority.device.domain.exception.InvalidPublicKeyFormatException;
import com.gridauthority.device.infrastructure.config.HcsDeviceProperties;
import com.gridauthority.device.infrastructure.hcs.AuthorityKeyMessage;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.hashgraph.sdk.TopicMessage;
import com.hedera.hashgraph.sdk.TopicMessageQuery;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


@Slf4j
@Service
@RequiredArgsConstructor
public class HcsKeyResolver {

    private final HcsDeviceProperties hcsDeviceProperties;
    private final ObjectMapper objectMapper;

    private Client hederaClient;

    @Getter private PublicKey resolvedPublicKey;
    @Getter private String resolvedKeyId     = "PENDING";
    @Getter private String resolvedAlgorithm = "PENDING";

    @PostConstruct
    public void resolveOnStartup() {
        if (!hcsDeviceProperties.isEnabled()) {
            log.warn("[HCS_DEVICE] Disabled — public key will NOT be resolved from HCS. " +
                    "Set hcs.enabled=true in production.");
            return;
        }
        if (isTopicUnconfigured()) {
            log.error("[HCS_DEVICE] hcs.public-key-topic-id not set — " +
                    "cannot resolve coordinator public key.");
            return;
        }
        try {
            hederaClient = buildClient();
            resolveKeyFromTopic();
        } catch (Exception e) {
            log.error("[HCS_DEVICE] Failed to resolve public key from topic={}: {}",
                    hcsDeviceProperties.getPublicKeyTopicId(), e.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        if (hederaClient != null) {
            try { hederaClient.close(); }
            catch (TimeoutException e) {
                log.warn("[HCS_DEVICE] Timeout closing Hedera client: {}", e.getMessage());
            }
        }
    }

    public boolean isKeyLoaded() { return resolvedPublicKey != null; }

    private void resolveKeyFromTopic() throws Exception {
        TopicId topicId = TopicId.fromString(hcsDeviceProperties.getPublicKeyTopicId());
        int maxMessages = hcsDeviceProperties.getMaxMessagesToScan();
        List<AuthorityKeyMessage> collected = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        log.info("[HCS_DEVICE] Subscribing to publicKeyTopicId={} network={} maxMessages={}",
                hcsDeviceProperties.getPublicKeyTopicId(),
                hcsDeviceProperties.getNetwork(), maxMessages);

        new TopicMessageQuery()
                .setTopicId(topicId)
                .setStartTime(Instant.EPOCH)
                .subscribe(hederaClient,
                        msg -> handleMessage(msg, collected, latch, maxMessages));

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        if (!completed) {
            log.debug("[HCS_DEVICE] Scan window elapsed — processing {} messages",
                    collected.size());
        }
        applyLatestKey(collected);
    }

    private void handleMessage(TopicMessage message,
                               List<AuthorityKeyMessage> collected,
                               CountDownLatch latch, int maxMessages) {
        try {
            String json = new String(message.contents, StandardCharsets.UTF_8);
            AuthorityKeyMessage msg = objectMapper.readValue(json, AuthorityKeyMessage.class);

            if (msg.isAuthorityKeyPublished()) {
                collected.add(msg);
                log.debug("[HCS_DEVICE] Found AUTHORITY_KEY_PUBLISHED — keyId={} payloadHash={}",
                        msg.keyId(), msg.payloadHash());
            }
            if (collected.size() >= maxMessages) latch.countDown();

        } catch (Exception e) {
            log.debug("[HCS_DEVICE] Skipping non-AUTHORITY_KEY_PUBLISHED message: {}",
                    e.getMessage());
        }
    }

    private void applyLatestKey(List<AuthorityKeyMessage> collected) {
        collected.stream()
                .max(Comparator.comparingLong(AuthorityKeyMessage::timestamp))
                .ifPresentOrElse(latest -> {
                            resolvedPublicKey = buildEcPublicKey(latest.publicKeyBase64());
                            resolvedKeyId     = latest.keyId();
                            resolvedAlgorithm = latest.signingAlgorithm();
                            log.info("[HCS_DEVICE] Public key resolved — keyId={} algorithm={}",
                                    resolvedKeyId, resolvedAlgorithm);
                        }, () ->
                                log.warn("[HCS_DEVICE] No AUTHORITY_KEY_PUBLISHED found in topic={}",
                                        hcsDeviceProperties.getPublicKeyTopicId())
                );
    }

    private PublicKey buildEcPublicKey(String publicKeyBase64) {
        try {
            byte[] derBytes = Base64.getDecoder().decode(publicKeyBase64);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(derBytes);
            return KeyFactory.getInstance("EC").generatePublic(keySpec);
        } catch (Exception e) {
            throw new InvalidPublicKeyFormatException(
                    "Invalid EC DER key from HCS: " + e.getMessage(), e);
        }
    }

    private boolean isTopicUnconfigured() {
        String id = hcsDeviceProperties.getPublicKeyTopicId();
        return id == null || id.isBlank();
    }

    private Client buildClient() {
        return switch (hcsDeviceProperties.getNetwork()) {
            case "mainnet"    -> Client.forMainnet();
            case "previewnet" -> Client.forPreviewnet();
            default           -> Client.forTestnet();
        };
    }
}
package com.gridauthority.device.aplication.service;

import com.gridauthority.device.domain.exception.InvalidPublicKeyFormatException;
import com.gridauthority.device.infrastructure.config.DeviceSimulationProperties;
import com.gridauthority.device.infrastructure.config.HcsDeviceProperties;
import com.gridauthority.device.infrastructure.hcs.AuthorityKeyMessage;
import com.hedera.hashgraph.sdk.*;
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
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
@Getter
public class HcsKeyResolver {

    private final HcsDeviceProperties hcsDeviceProperties;
    private final DeviceSimulationProperties deviceSimulationProperties;
    private final ObjectMapper objectMapper;

    private Client hederaClient;

    private final AtomicReference<PublicKey> resolvedPublicKeyRef = new AtomicReference<>();
    private final AtomicReference<Instant> activeKeyTimestamp = new AtomicReference<>(Instant.EPOCH);

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
            resolveInitialKey();
            startPersistentWatch();
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

    public PublicKey getResolvedPublicKey()  { return resolvedPublicKeyRef.get(); }

    private void resolveInitialKey() throws Exception {
        TopicId topicId    = TopicId.fromString(hcsDeviceProperties.getPublicKeyTopicId());
        int maxMessages    = hcsDeviceProperties.getMaxMessagesToScan();
        List<AuthorityKeyMessage> collected = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        log.info("[HCS_DEVICE] Boot scan — publicKeyTopicId={} network={} maxMessages={}",
                hcsDeviceProperties.getPublicKeyTopicId(),
                hcsDeviceProperties.getNetwork(), maxMessages);

        new TopicMessageQuery()
                .setTopicId(topicId)
                .setStartTime(Instant.EPOCH)
                .subscribe(hederaClient,
                        msg -> handleBootMessage(msg, collected, latch, maxMessages));

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        if (!completed) {
            log.debug("[HCS_DEVICE] Boot scan window elapsed — {} messages collected", collected.size());
        }
        applyLatestKey(collected);
    }

    private void handleBootMessage(TopicMessage message,
                                   List<AuthorityKeyMessage> collected,
                                   CountDownLatch latch, int maxMessages) {
        try {
            String json = new String(message.contents, StandardCharsets.UTF_8);
            AuthorityKeyMessage msg = objectMapper.readValue(json, AuthorityKeyMessage.class);
            if (msg.isAuthorityKeyPublished()) {
                collected.add(msg);
                log.debug("[HCS_DEVICE] Boot scan found AUTHORITY_KEY_PUBLISHED — keyId={}", msg.keyId());
            }
            if (collected.size() >= maxMessages) latch.countDown();
        } catch (Exception e) {
            log.debug("[HCS_DEVICE] Boot scan skipping message: {}", e.getMessage());
        }
    }

    private void applyLatestKey(List<AuthorityKeyMessage> collected) {
        collected.stream()
                .max(Comparator.comparingLong(AuthorityKeyMessage::timestamp))
                .ifPresentOrElse(
                        latest -> applyKey(latest, null),
                        () -> log.warn("[HCS_DEVICE] No AUTHORITY_KEY_PUBLISHED found in topic={}",
                                hcsDeviceProperties.getPublicKeyTopicId())
                );
    }

    private void startPersistentWatch() {
        TopicId topicId   = TopicId.fromString(hcsDeviceProperties.getPublicKeyTopicId());
        Instant startFrom = activeKeyTimestamp.get().plusMillis(1);

        log.info("[HCS_DEVICE] Starting persistent watch — publicKeyTopicId={} startFrom={}",
                hcsDeviceProperties.getPublicKeyTopicId(), startFrom);

        new TopicMessageQuery()
                .setTopicId(topicId)
                .setStartTime(startFrom)
                .subscribe(hederaClient, this::handleLiveMessage);
    }

    private void handleLiveMessage(TopicMessage message) {
        try {
            String json = new String(message.contents, StandardCharsets.UTF_8);
            AuthorityKeyMessage msg = objectMapper.readValue(json, AuthorityKeyMessage.class);

            if (!msg.isAuthorityKeyRotation()) return;

            Instant msgInstant = Instant.ofEpochMilli(msg.timestamp());
            if (!msgInstant.isAfter(activeKeyTimestamp.get())) {
                log.debug("[HCS_DEVICE] Live message ignored — not newer than active key (keyId={})", msg.keyId());
                return;
            }

            log.info("[HCS_DEVICE] Key rotation detected on HCS — applying new keyId={}", msg.keyId());
            applyKey(msg, message.consensusTimestamp);

        } catch (Exception e) {
            log.debug("[HCS_DEVICE] Live watch skipping message: {}", e.getMessage());
        }
    }

    private void applyKey(AuthorityKeyMessage msg, Instant consensusTimestamp) {
        PublicKey publicKey = buildEcPublicKey(msg.publicKeyBase64());

        resolvedPublicKeyRef.set(publicKey);

        Instant effectiveTimestamp = consensusTimestamp != null
                ? consensusTimestamp
                : Instant.ofEpochMilli(msg.timestamp());
        activeKeyTimestamp.set(effectiveTimestamp);

        log.info("[HCS_DEVICE] Public key applied — keyId={} algorithm={} effectiveAt={}",
                msg.keyId(), msg.signingAlgorithm(), effectiveTimestamp);

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
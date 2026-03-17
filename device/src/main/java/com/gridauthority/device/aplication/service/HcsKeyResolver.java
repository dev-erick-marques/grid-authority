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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
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
    private final AtomicReference<Instant> keyActiveFrom = new AtomicReference<>(Instant.EPOCH);

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

    public boolean isKeyActive() {
        if (resolvedPublicKeyRef.get() == null) return false;
        return Instant.now().compareTo(keyActiveFrom.get()) >= 0;
    }

    public Instant getKeyActiveFrom() { return keyActiveFrom.get(); }

    private void resolveInitialKey() throws Exception {
        TopicId topicId = TopicId.fromString(hcsDeviceProperties.getPublicKeyTopicId());
        int maxMessages = hcsDeviceProperties.getMaxMessagesToScan();

        List<BootEntry> collected = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        log.info("[HCS_DEVICE] Boot scan — publicKeyTopicId={} network={} maxMessages={}",
                hcsDeviceProperties.getPublicKeyTopicId(),
                hcsDeviceProperties.getNetwork(), maxMessages);

        AtomicInteger totalSeen = new AtomicInteger(0);

        new TopicMessageQuery()
                .setTopicId(topicId)
                .setStartTime(Instant.EPOCH)
                .subscribe(hederaClient,
                        message -> handleBootMessage(message, collected, latch, maxMessages, totalSeen));

        boolean completed = latch.await(10, TimeUnit.SECONDS);
        if (!completed) {
            log.debug("[HCS_DEVICE] Boot scan window elapsed — {} total messages seen, {} authority key events collected",
                    totalSeen.get(), collected.size());
        }
        applyLatestKey(collected);
    }

    private record BootEntry(AuthorityKeyMessage msg, TopicMessage raw) {}

    private void handleBootMessage(TopicMessage message,
                                   List<BootEntry> collected,
                                   CountDownLatch latch,
                                   int maxMessages,
                                   AtomicInteger totalSeen) {
        int seen = totalSeen.incrementAndGet();
        try {
            AuthorityKeyMessage msg = parseAuthorityKeyMessage(message);
            if (msg != null && msg.isAuthorityKeyPublished()) {
                collected.add(new BootEntry(msg, message));
                log.debug("[HCS_DEVICE] Boot scan found {} — keyId={} consensusTimestamp={}",
                        msg.eventType(), msg.keyId(), message.consensusTimestamp);
            }
        } catch (Exception e) {
            log.debug("[HCS_DEVICE] Boot scan skipping message: {}", e.getMessage());
        }
        if (seen >= maxMessages) latch.countDown();
    }

    private void applyLatestKey(List<BootEntry> collected) {
        collected.stream()
                .max(Comparator.comparing(e -> e.raw().consensusTimestamp))
                .ifPresentOrElse(
                        latest -> applyKey(latest.msg(), latest.raw().consensusTimestamp),
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
            AuthorityKeyMessage msg = parseAuthorityKeyMessage(message);
            if (msg == null || !msg.isRotationKey()) return;

            if (!message.consensusTimestamp.isAfter(activeKeyTimestamp.get())) {
                log.debug("[HCS_DEVICE] Live message ignored — not newer than active key (keyId={})", msg.keyId());
                return;
            }

            log.info("[HCS_DEVICE] Key rotation detected on HCS — applying new keyId={}", msg.keyId());
            applyKey(msg, message.consensusTimestamp);

        } catch (Exception e) {
            log.debug("[HCS_DEVICE] Live watch skipping message: {}", e.getMessage());
        }
    }

    private AuthorityKeyMessage parseAuthorityKeyMessage(TopicMessage message) throws Exception {
        String json = new String(message.contents, StandardCharsets.UTF_8);
        var node = objectMapper.readTree(json);
        var payloadNode = node.get("payload");
        if (payloadNode == null || payloadNode.isNull()) return null;
        return objectMapper.treeToValue(payloadNode, AuthorityKeyMessage.class);
    }

    private void applyKey(AuthorityKeyMessage msg, Instant consensusTimestamp) {
        PublicKey publicKey = buildEcPublicKey(msg.publicKeyBase64());

        Instant activeFrom = consensusTimestamp.plusMillis(msg.activationWindowMs());

        resolvedPublicKeyRef.set(publicKey);
        activeKeyTimestamp.set(consensusTimestamp);
        keyActiveFrom.set(activeFrom);

        log.info("[HCS_DEVICE] Public key applied — keyId={} algorithm={} consensusTimestamp={} activationWindowMs={} activeFrom={}",
                msg.keyId(), msg.signingAlgorithm(), consensusTimestamp,
                msg.activationWindowMs(), activeFrom);
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
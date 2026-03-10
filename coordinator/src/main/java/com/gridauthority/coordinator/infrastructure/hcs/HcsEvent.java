package com.gridauthority.coordinator.infrastructure.hcs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HcsEvent(

        @JsonProperty("eventType")
        String eventType,

        @JsonProperty("deviceId")
        String deviceId,

        @JsonProperty("action")
        String action,

        @JsonProperty("reason")
        String reason,

        @JsonProperty("cv")
        Double cv,

        @JsonProperty("payloadHash")
        String payloadHash,

        @JsonProperty("keyId")
        String keyId,

        @JsonProperty("signingAlgorithm")
        String signingAlgorithm,

        @JsonProperty("publicKeyBase64")
        String publicKeyBase64,

        @JsonProperty("signatureBase64")
        String signatureBase64,

        @JsonProperty("timestamp")
        long timestamp
) {

    public static HcsEvent authorityKeyPublished(
            String keyId, String signingAlgorithm,
            String publicKeyBase64, String payloadHash) {
        return HcsEvent.builder()
                .eventType(EventType.AUTHORITY_KEY_PUBLISHED_ON_BOOT.name())
                .keyId(keyId)
                .signingAlgorithm(signingAlgorithm)
                .publicKeyBase64(publicKeyBase64)
                .payloadHash(payloadHash)
                .timestamp(now())
                .build();
    }

    public static HcsEvent decision(
            String deviceId, String action, String reason, Double cv,
            String payloadHash, String keyId, String signingAlgorithm,
            String signatureBase64) {
        return HcsEvent.builder()
                .eventType(EventType.DECISION.name())
                .deviceId(deviceId)
                .action(action)
                .reason(reason)
                .cv(cv)
                .payloadHash(payloadHash)
                .keyId(keyId)
                .signingAlgorithm(signingAlgorithm)
                .signatureBase64(signatureBase64)
                .timestamp(now())
                .build();
    }

    public static HcsEvent surge(
            String deviceId, String action, String payloadHash,
            String keyId, String signingAlgorithm, String signatureBase64) {
        return HcsEvent.builder()
                .eventType(EventType.SURGE.name())
                .deviceId(deviceId)
                .action(action)
                .payloadHash(payloadHash)
                .keyId(keyId)
                .signingAlgorithm(signingAlgorithm)
                .signatureBase64(signatureBase64)
                .timestamp(now())
                .build();
    }

    private static long now() { return System.currentTimeMillis(); }

    public enum EventType {
        AUTHORITY_KEY_PUBLISHED_ON_BOOT,
        DECISION,
        SURGE
    }
}
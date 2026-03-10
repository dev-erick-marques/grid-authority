package com.gridauthority.coordinator.infrastructure.hcs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

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
        return new HcsEvent(
                EventType.AUTHORITY_KEY_PUBLISHED_ON_BOOT.name(),
                null, null, null, null,
                payloadHash, keyId, signingAlgorithm, publicKeyBase64,
                null, now()
        );
    }

    public static HcsEvent decision(
            String deviceId, String action, String reason, Double cv,
            String payloadHash, String keyId, String signingAlgorithm,
            String signatureBase64) {
        return new HcsEvent(
                EventType.DECISION.name(),
                deviceId, action, reason, cv,
                payloadHash, keyId, signingAlgorithm, null,
                signatureBase64, now()
        );
    }

    public static HcsEvent surge(
            String deviceId, String action, String payloadHash,
            String keyId, String signingAlgorithm, String signatureBase64) {
        return new HcsEvent(
                EventType.SURGE.name(),
                deviceId, action, null, null,
                payloadHash, keyId, signingAlgorithm, null,
                signatureBase64, now()
        );
    }

    private static long now() { return System.currentTimeMillis(); }

    public enum EventType {
        AUTHORITY_KEY_PUBLISHED_ON_BOOT,
        DECISION,
        SURGE
    }
}
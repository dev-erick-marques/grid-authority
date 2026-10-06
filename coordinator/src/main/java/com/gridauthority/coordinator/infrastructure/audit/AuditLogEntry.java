package com.gridauthority.coordinator.infrastructure.audit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gridauthority.coordinator.infrastructure.hcs.HcsEvent;
import com.gridauthority.coordinator.infrastructure.hcs.HcsPayload;
import lombok.Builder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record AuditLogEntry(

        @JsonProperty("type")      String type,       // KMS_SIGNED | HCS_ANCHORED | HCS_ERROR
        @JsonProperty("eventType") String eventType,  // DECISION | SURGE | AUTHORITY_KEY_PUBLISHED_ON_BOOT
        @JsonProperty("deviceId")  String deviceId,
        @JsonProperty("action")    String action,
        @JsonProperty("keyId")     String keyId,
        @JsonProperty("topicId")   String topicId,
        @JsonProperty("sha256")    String sha256,
        @JsonProperty("error")     String error,
        @JsonProperty("ts")        long ts
) {
    public static AuditLogEntry kmsSigned(String deviceId, String action, String keyId) {
        return AuditLogEntry.builder()
                .type("KMS_SIGNED")
                .deviceId(deviceId)
                .action(action)
                .keyId(keyId)
                .ts(System.currentTimeMillis())
                .build();
    }

    public static AuditLogEntry fromHcsEvent(HcsEvent event, String type, String topicId) {
        HcsPayload p = event.payload();
        return AuditLogEntry.builder()
                .type(type)
                .eventType(p.eventType())
                .deviceId(p.deviceId())
                .action(p.action())
                .keyId(p.keyId())
                .topicId(topicId)
                .sha256(event.sha256())
                .ts(p.timestamp())
                .build();
    }
}
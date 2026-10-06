package com.gridauthority.coordinator.infrastructure.audit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record AuditLogEntry(
        @JsonProperty("type") String type,
        @JsonProperty("eventType") String eventType,
        @JsonProperty("deviceId") String deviceId,
        @JsonProperty("action") String action,
        @JsonProperty("policyVersion") Long policyVersion,
        @JsonProperty("riskScore") Double riskScore,
        @JsonProperty("confidence") Double confidence,
        @JsonProperty("timeToThreshold") Double timeToThreshold,
        @JsonProperty("observabilityScore") Double observabilityScore,
        @JsonProperty("metricsHash") String metricsHash,
        @JsonProperty("commandHash") String commandHash,
        @JsonProperty("ts") long ts) {
}

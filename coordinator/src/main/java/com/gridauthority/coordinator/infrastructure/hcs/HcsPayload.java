package com.gridauthority.coordinator.infrastructure.hcs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HcsPayload(

    @JsonProperty("eventType") String eventType,
    @JsonProperty("deviceId") String deviceId,
    @JsonProperty("action") String action,
    @JsonProperty("reason") String reason,
    @JsonProperty("mean") Double mean,
    @JsonProperty("cv") Double cv,
    @JsonProperty("std") Double std,
    @JsonProperty("keyId") String keyId,
    @JsonProperty("signingAlgorithm") String signingAlgorithm,
    @JsonProperty("publicKeyBase64") String publicKeyBase64,
    @JsonProperty("signatureBase64") String signatureBase64,
    @JsonProperty("timestamp") long timestamp,

    @JsonProperty("activationWindowMs") long activationWindowMs
) {}

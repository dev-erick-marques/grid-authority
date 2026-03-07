package com.gridauthority.coordinator.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SignedCommandPayload(
        @JsonProperty("deviceId") String deviceId,
        @JsonProperty("action") String action,
        @JsonProperty("issuedAt") long issuedAt,
        @JsonProperty("keyId") String keyId,
        @JsonProperty("signingAlgorithm") String signingAlgorithm,
        @JsonProperty("signatureBase64") String signatureBase64,
        @JsonProperty("canonicalJson") String canonicalJson
) {}
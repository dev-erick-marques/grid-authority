package com.gridauthority.device.aplication.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gridauthority.device.domain.model.SurgeAction;

public record SignedCommandDTO(
        @JsonProperty("deviceId") String deviceId,
        @JsonProperty("action") SurgeAction action,
        @JsonProperty("issuedAt") long   issuedAt,
        @JsonProperty("keyId") String keyId,
        @JsonProperty("signingAlgorithm") String signingAlgorithm,
        @JsonProperty("signatureBase64") String signatureBase64,
        @JsonProperty("canonicalJson") String canonicalJson
) {}
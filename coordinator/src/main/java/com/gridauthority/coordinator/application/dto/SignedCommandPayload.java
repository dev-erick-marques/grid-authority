package com.gridauthority.coordinator.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SignedCommandPayload(
        @JsonProperty("signatureBase64") String signatureBase64,
        @JsonProperty("canonicalJson") String canonicalJson
) {}
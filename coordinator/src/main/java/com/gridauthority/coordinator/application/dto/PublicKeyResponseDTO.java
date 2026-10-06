package com.gridauthority.coordinator.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PublicKeyResponseDTO(
        @JsonProperty("keyId")           String keyId,
        @JsonProperty("signingAlgorithm") String signingAlgorithm,
        @JsonProperty("publicKeyBase64") String publicKeyBase64
) {}
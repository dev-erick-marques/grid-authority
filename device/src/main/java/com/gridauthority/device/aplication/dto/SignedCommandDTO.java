package com.gridauthority.device.aplication.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record SignedCommandDTO(

        @NotBlank(message = "deviceId must not be blank")
        @JsonProperty("deviceId") String deviceId,

        @NotBlank(message = "action must not be blank")
        @JsonProperty("action") String action,

        @Positive(message = "issuedAt must be a positive timestamp")
        @JsonProperty("issuedAt") long issuedAt,

        @NotBlank(message = "keyId must not be blank")
        @JsonProperty("keyId") String keyId,

        @NotBlank(message = "signingAlgorithm must not be blank")
        @JsonProperty("signingAlgorithm") String signingAlgorithm,

        @NotBlank(message = "signatureBase64 must not be blank")
        @JsonProperty("signatureBase64") String signatureBase64,

        @NotBlank(message = "canonicalJson must not be blank")
        @JsonProperty("canonicalJson") String canonicalJson
) {}
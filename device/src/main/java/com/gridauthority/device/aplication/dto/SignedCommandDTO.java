package com.gridauthority.device.aplication.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record SignedCommandDTO(

        @NotBlank(message = "signatureBase64 must not be blank")
        @JsonProperty("signatureBase64") String signatureBase64,

        @NotBlank(message = "canonicalJson must not be blank")
        @JsonProperty("canonicalJson") String canonicalJson
) {}
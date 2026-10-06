package com.gridauthority.coordinator.infrastructure.kms;

import com.gridauthority.coordinator.application.dto.SignedCommandPayload;

public record SigningResult(
        SignedCommandPayload payload,
        CommandSigningContext context,
        String keyId,
        String signingAlgorithm
) {}
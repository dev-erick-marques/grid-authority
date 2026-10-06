package com.gridauthority.coordinator.infrastructure.kms;

public record CommandSigningContext(
        String action,
        String commandId,
        String deviceId,
        long issuedAt
) {}

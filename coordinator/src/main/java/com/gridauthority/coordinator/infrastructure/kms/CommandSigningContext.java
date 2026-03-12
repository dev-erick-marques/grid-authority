package com.gridauthority.coordinator.infrastructure.kms;

public record CommandSigningContext(
        String action,
        String deviceId,
        long issuedAt
) {}

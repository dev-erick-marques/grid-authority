package com.gridauthority.coordinator.application.dto;

public record CommandSigningContext(
        String action,
        String deviceId,
        long issuedAt
) {}

package com.gridauthority.device.aplication.dto;

public record CommandSigningContext(
        String action,
        String deviceId,
        long issuedAt
) {}

package com.gridauthority.device.aplication.dto;

public record CommandSigningContext(
        String action,
        String commandId,
        String deviceId,
        long issuedAt
) {}

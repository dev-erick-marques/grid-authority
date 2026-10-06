package com.gridauthority.coordinator.application.dto;
public record SignedCommandPayload(String signatureBase64, String canonicalJson) {}

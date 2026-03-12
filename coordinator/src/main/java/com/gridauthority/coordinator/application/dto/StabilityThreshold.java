package com.gridauthority.coordinator.application.dto;

public record StabilityThreshold(
        double thresholdCV,
        String standard,
        String description
) {}
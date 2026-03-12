package com.gridauthority.coordinator.application.dto;

public record HcsTopicsDTO(
        String network,
        String publicKeyTopicId,
        String decisionTopicId,
        String surgeTopicId
) {}

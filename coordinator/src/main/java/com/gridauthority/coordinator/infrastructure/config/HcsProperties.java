package com.gridauthority.coordinator.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "hcs")
public class HcsProperties {

    private boolean enabled = false;
    private String network = "testnet";
    private String accountId;
    private String privateKey;
    private String decisionTopicId;
    private String surgeTopicId;
    private String publicKeyTopicId;

}
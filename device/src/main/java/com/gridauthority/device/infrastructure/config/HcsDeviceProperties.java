package com.gridauthority.device.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "hcs")
public class HcsDeviceProperties {

    private String network = "testnet";
    private int maxMessagesToScan = 100;
    private long keyActivationGraceMs = 0;
    private String publicKeyTopicId;
}
package com.gridauthority.device.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "device.network")
public class DeviceNetworkProperties {
    private String url;

    public String getSourceUrl() {
        return url;
    }
}
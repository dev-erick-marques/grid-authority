package com.gridauthority.device.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "coordinator")
public class CoordinatorProperties {
    private String url;
}
package com.gridauthority.coordinator.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "authority")
public class AuthoritySigningProperties {

    private String keyId = "local-authority";
    private String privateKeyBase64 = "";
    private long activationWindowMs = 0;
}

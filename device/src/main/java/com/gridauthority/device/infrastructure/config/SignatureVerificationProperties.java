package com.gridauthority.device.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "command.verification")
public class SignatureVerificationProperties {

    private boolean enabled = false;
    private long timestampToleranceMs = 30_000;
}
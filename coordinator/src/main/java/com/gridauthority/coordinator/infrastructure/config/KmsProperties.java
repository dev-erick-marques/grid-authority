package com.gridauthority.coordinator.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kms")
public class KmsProperties {

    private String keyId;
    private String region = "us-east-1";
    private String signingAlgorithm = "ECDSA_SHA_256";
    private boolean enabled = true;
}
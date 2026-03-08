package com.gridauthority.coordinator.infrastructure.config;

import com.gridauthority.coordinator.domain.exceptions.KmsConfigurationException;
import jakarta.annotation.PostConstruct;
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

    @PostConstruct
    public void validate() {
        if (enabled && (keyId == null || keyId.isBlank())) {
            throw new KmsConfigurationException(
                    "[KmsProperties] kms.key-id must be set when kms.enabled=true. " +
                            "Set KMS_KEY_ID environment variable."
            );
        }
    }
}
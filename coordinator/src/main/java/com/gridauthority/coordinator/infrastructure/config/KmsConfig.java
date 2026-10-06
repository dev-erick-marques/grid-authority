package com.gridauthority.coordinator.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;

@Configuration
@RequiredArgsConstructor
public class KmsConfig {

    private final KmsProperties kmsProperties;

    @Bean
    public KmsClient kmsClient() {
        return KmsClient.builder()
                .region(Region.of(kmsProperties.getRegion()))
                .build();
    }
}
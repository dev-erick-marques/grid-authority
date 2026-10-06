package com.gridauthority.coordinator.signing;

import com.gridauthority.coordinator.infrastructure.config.AuthoritySigningProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AuthoritySigningProperties.class)
public class SigningConfig {

    @Bean
    public LocalSigningService localSigningService(AuthoritySigningProperties p) {
        return new LocalSigningService(p);
    }
}

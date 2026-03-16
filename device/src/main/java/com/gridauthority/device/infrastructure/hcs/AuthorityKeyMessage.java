package com.gridauthority.device.infrastructure.hcs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthorityKeyMessage(
        @JsonProperty("eventType") String eventType,
        @JsonProperty("keyId") String keyId,
        @JsonProperty("signingAlgorithm") String signingAlgorithm,
        @JsonProperty("publicKeyBase64") String publicKeyBase64,
        @JsonProperty("payloadHash") String payloadHash,
        @JsonProperty("timestamp") long timestamp
) {
    public boolean isAuthorityKeyPublished() {
        return "AUTHORITY_KEY_PUBLISHED_ON_BOOT".equals(eventType);
    }
    public boolean isAuthorityKeyRotation() {
        return "AUTHORITY_KEY_PUBLISHED_ON_ROTATION".equals(eventType);
    }
}
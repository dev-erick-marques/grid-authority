package com.gridauthority.coordinator.infrastructure.hcs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import tools.jackson.databind.ObjectMapper;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HcsEvent(

        @JsonProperty("payload") HcsPayload payload,
        @JsonProperty("sha256") String sha256
) {

    public static HcsEvent of(HcsPayload payload, ObjectMapper mapper) {
        try {
            String payloadJson = mapper.writeValueAsString(payload);
            String hash = PayloadHasher.sha256(payloadJson);
            return new HcsEvent(payload, hash);
        } catch (Exception e) {
            return new HcsEvent(payload, "HASH_UNAVAILABLE");
        }
    }

    public enum EventType {
        AUTHORITY_KEY_PUBLISHED_ON_BOOT,
        DECISION,
        SURGE
    }
}
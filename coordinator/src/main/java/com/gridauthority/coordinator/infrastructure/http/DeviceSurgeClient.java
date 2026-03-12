package com.gridauthority.coordinator.infrastructure.http;

import com.gridauthority.coordinator.application.dto.SignedCommandPayload;
import com.gridauthority.coordinator.infrastructure.transport.SurgeTransport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "transport.mode", havingValue = "http", matchIfMissing = true)
public class DeviceSurgeClient implements SurgeTransport {

    private static final String SURGE_PATH = "/api/surge/signed";

    private final RestTemplate restTemplate;

    @Override
    public void send(String deviceBaseUrl, String deviceId,
                     String action, SignedCommandPayload payload) {
        String url = deviceBaseUrl + SURGE_PATH;
        try {
            restTemplate.postForLocation(url, payload);
            log.info("[SURGE-CLIENT] {} → device={} (signed keyId={})",
                    action, deviceId, payload.keyId());
        } catch (RestClientException e) {
            log.error("[SURGE-CLIENT] Failed {} → device={} — {}", action, deviceId, e.getMessage());
            throw e;
        }
    }
}
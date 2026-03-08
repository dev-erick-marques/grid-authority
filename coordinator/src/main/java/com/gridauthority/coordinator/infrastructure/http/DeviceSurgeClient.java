package com.gridauthority.coordinator.infrastructure.http;

import com.gridauthority.coordinator.application.dto.SignedCommandPayload;
import com.gridauthority.coordinator.infrastructure.kms.KmsSigningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceSurgeClient {

    private static final String SURGE_PATH = "/api/surge/signed";

    private final RestTemplate restTemplate;
    private final KmsSigningService kmsSigningService;

    public void startSurge(String deviceBaseUrl, String deviceId) {
        post(deviceBaseUrl, deviceId, "SURGE_START");
    }

    public void stopSurge(String deviceBaseUrl, String deviceId) {
        post(deviceBaseUrl, deviceId, "SURGE_STOP");
    }

    public void startCycle(String deviceBaseUrl, String deviceId) {
        post(deviceBaseUrl, deviceId, "SURGE_CYCLE_START");
    }

    public void stopCycle(String deviceBaseUrl, String deviceId) {
        post(deviceBaseUrl, deviceId, "SURGE_CYCLE_STOP");
    }

    private void post(String deviceBaseUrl, String deviceId, String action) {
        SignedCommandPayload payload = kmsSigningService.createSignedCommand(deviceId, action);
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
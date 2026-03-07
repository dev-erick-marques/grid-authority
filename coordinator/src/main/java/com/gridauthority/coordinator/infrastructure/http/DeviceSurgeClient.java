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

    private static final String SURGE_START_PATH    = "/api/surge/start";
    private static final String SURGE_STOP_PATH     = "/api/surge/stop";
    private static final String SURGE_CYCLE_START   = "/api/surge/cycle/start";
    private static final String SURGE_CYCLE_STOP    = "/api/surge/cycle/stop";

    private final KmsSigningService kmsSigningService;
    private final RestTemplate restTemplate;

    public void startSurge(String deviceBaseUrl, String deviceId) {
        post(deviceBaseUrl + SURGE_START_PATH, deviceId, "SURGE_START");
    }

    public void stopSurge(String deviceBaseUrl, String deviceId) {
        post(deviceBaseUrl + SURGE_STOP_PATH, deviceId, "SURGE_STOP");
    }

    public void startCycle(String deviceBaseUrl, String deviceId) {
        post(deviceBaseUrl + SURGE_CYCLE_START, deviceId, "SURGE_CYCLE_START");
    }

    public void stopCycle(String deviceBaseUrl, String deviceId) {
        post(deviceBaseUrl + SURGE_CYCLE_STOP, deviceId, "SURGE_CYCLE_STOP");
    }

    private void post(String url, String deviceId, String action) {
        SignedCommandPayload payload = kmsSigningService.sign(deviceId, action);
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
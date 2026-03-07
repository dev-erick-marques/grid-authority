package com.gridauthority.coordinator.infrastructure.http;

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
        try {
            restTemplate.postForLocation(url, null);
            log.info("[SURGE-CLIENT] {} → device={} at {}", action, deviceId, url);
        } catch (RestClientException e) {
            log.error("[SURGE-CLIENT] Failed {} → device={} at {} — {}", action, deviceId, url, e.getMessage());
            throw e;
        }
    }
}
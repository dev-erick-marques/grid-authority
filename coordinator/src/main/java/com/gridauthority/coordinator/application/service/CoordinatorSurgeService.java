package com.gridauthority.coordinator.application.service;

import com.gridauthority.coordinator.domain.model.DeviceSurgeState;
import com.gridauthority.coordinator.infrastructure.http.DeviceSurgeClient;
import com.gridauthority.coordinator.infrastructure.registry.DeviceRegistry;
import com.gridauthority.coordinator.infrastructure.repository.DeviceSurgeStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoordinatorSurgeService {

    private final DeviceRegistry deviceRegistry;
    private final DeviceSurgeClient deviceSurgeClient;
    private final DeviceSurgeStateRepository surgeStateRepository;

    public DeviceSurgeState getSurgeState(String deviceId) {
        return surgeStateRepository.get(deviceId);
    }

    public void startSurge(String deviceId) {
        dispatch(deviceId, DeviceSurgeState.SURGE_ACTIVE,
                url -> deviceSurgeClient.startSurge(url, deviceId),
                "SURGE_START");
    }

    public void stopSurge(String deviceId) {
        dispatch(deviceId, DeviceSurgeState.INACTIVE,
                url -> deviceSurgeClient.stopSurge(url, deviceId),
                "SURGE_STOP");
    }

    public void startCycle(String deviceId) {
        dispatch(deviceId, DeviceSurgeState.CYCLE_ACTIVE,
                url -> deviceSurgeClient.startCycle(url, deviceId),
                "SURGE_CYCLE_START");
    }

    public void stopCycle(String deviceId) {
        dispatch(deviceId, DeviceSurgeState.INACTIVE,
                url -> deviceSurgeClient.stopCycle(url, deviceId),
                "SURGE_CYCLE_STOP");
    }

    private void dispatch(String deviceId, DeviceSurgeState nextState, SurgeAction action, String label) {
        deviceRegistry.resolve(deviceId).ifPresentOrElse(baseUrl -> {
            try {
                action.execute(baseUrl);
                surgeStateRepository.set(deviceId, nextState);
                log.info("[SURGE] {} → device={} state={}", label, deviceId, nextState);
            } catch (RestClientException e) {
                log.error("[SURGE] {} failed → device={} — {}", label, deviceId, e.getMessage());
                throw e;
            }
        }, () -> {
            log.warn("[SURGE] {} → device={} not registered in registry", label, deviceId);
            throw new IllegalStateException("Device not registered: " + deviceId);
        });
    }

    @FunctionalInterface
    private interface SurgeAction {
        void execute(String baseUrl);
    }
}
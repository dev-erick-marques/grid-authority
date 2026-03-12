package com.gridauthority.coordinator.application.service;

import com.gridauthority.coordinator.application.dto.SignedCommandPayload;
import com.gridauthority.coordinator.domain.model.DeviceSurgeState;
import com.gridauthority.coordinator.infrastructure.hcs.HcsAnchorService;
import com.gridauthority.coordinator.infrastructure.hcs.HcsEvent;
import com.gridauthority.coordinator.infrastructure.hcs.HcsPayload;
import com.gridauthority.coordinator.infrastructure.kms.KmsSigningService;
import com.gridauthority.coordinator.infrastructure.registry.DeviceRegistry;
import com.gridauthority.coordinator.infrastructure.repository.DeviceSurgeStateRepository;
import com.gridauthority.coordinator.infrastructure.transport.SurgeTransport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoordinatorSurgeService {

    private final DeviceRegistry deviceRegistry;
    private final SurgeTransport surgeTransport;
    private final DeviceSurgeStateRepository surgeStateRepository;
    private final KmsSigningService kmsSigningService;
    private final HcsAnchorService hcsAnchorService;
    private final ObjectMapper objectMapper;

    public DeviceSurgeState getSurgeState(String deviceId) {
        return surgeStateRepository.get(deviceId);
    }

    public void startSurge(String deviceId) {
        dispatch(deviceId, DeviceSurgeState.SURGE_ACTIVE, "SURGE_START");
    }

    public void stopSurge(String deviceId) {
        dispatch(deviceId, DeviceSurgeState.INACTIVE, "SURGE_STOP");
    }

    public void startCycle(String deviceId) {
        dispatch(deviceId, DeviceSurgeState.CYCLE_ACTIVE, "SURGE_CYCLE_START");
    }

    public void stopCycle(String deviceId) {
        dispatch(deviceId, DeviceSurgeState.INACTIVE, "SURGE_CYCLE_STOP");
    }

    private void dispatch(String deviceId, DeviceSurgeState nextState, String action) {
        SignedCommandPayload signed = kmsSigningService.issueCommand(deviceId, action);

        deviceRegistry.resolve(deviceId).ifPresentOrElse(baseUrl -> {
            try {
                surgeTransport.send(baseUrl, deviceId, action, signed);
                surgeStateRepository.set(deviceId, nextState);
                log.info("[SURGE] {} → device={} state={}", action, deviceId, nextState);
                anchorSurge(deviceId, action, signed);
            } catch (RestClientException e) {
                log.error("[SURGE] {} failed → device={} — {}", action, deviceId, e.getMessage());
                throw e;
            }
        }, () -> {
            log.warn("[SURGE] {} → device={} not registered", action, deviceId);
            throw new IllegalStateException("Device not registered: " + deviceId);
        });
    }

    private void anchorSurge(String deviceId, String action, SignedCommandPayload signed) {
        HcsPayload payload = HcsPayload.builder()
                .eventType(HcsEvent.EventType.SURGE.name())
                .deviceId(deviceId)
                .action(action)
                .keyId(signed.keyId())
                .signingAlgorithm(signed.signingAlgorithm())
                .signatureBase64(signed.signatureBase64())
                .timestamp(signed.issuedAt())
                .build();

        hcsAnchorService.anchorSurge(HcsEvent.of(payload, objectMapper));
    }
}
package com.gridauthority.coordinator.application.service;

import com.gridauthority.coordinator.application.dto.SignedCommandPayload;
import com.gridauthority.coordinator.domain.model.DeviceSurgeState;
import com.gridauthority.coordinator.infrastructure.hcs.HcsAnchorService;
import com.gridauthority.coordinator.infrastructure.hcs.HcsEvent;
import com.gridauthority.coordinator.infrastructure.http.DeviceSurgeClient;
import com.gridauthority.coordinator.infrastructure.kms.KmsSigningService;
import com.gridauthority.coordinator.infrastructure.registry.DeviceRegistry;
import com.gridauthority.coordinator.infrastructure.repository.DeviceSurgeStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoordinatorSurgeService {

    private final DeviceRegistry deviceRegistry;
    private final DeviceSurgeClient deviceSurgeClient;
    private final DeviceSurgeStateRepository surgeStateRepository;
    private final KmsSigningService kmsSigningService;
    private final HcsAnchorService hcsAnchorService;

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
        SignedCommandPayload signed = kmsSigningService.createSignedCommand(deviceId, action);

        deviceRegistry.resolve(deviceId).ifPresentOrElse(baseUrl -> {
            try {
                deviceSurgeClient.send(baseUrl, deviceId, action, signed);
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
        HcsEvent event = HcsEvent.surge(
                deviceId,
                action,
                sha256(signed.canonicalJson()),
                signed.keyId(),
                signed.signingAlgorithm(),
                signed.signatureBase64()
        );
        hcsAnchorService.anchorSurge(event);
    }
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return "HASH_UNAVAILABLE";
        }
    }

    @FunctionalInterface
    private interface SurgeAction {
        void execute(String baseUrl);
    }
}
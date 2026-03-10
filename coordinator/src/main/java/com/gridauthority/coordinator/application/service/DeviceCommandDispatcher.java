package com.gridauthority.coordinator.application.service;

import com.gridauthority.coordinator.application.dto.DeviceMetricsDTO;
import com.gridauthority.coordinator.application.dto.SignedCommandPayload;
import com.gridauthority.coordinator.domain.model.DeviceCommand;
import com.gridauthority.coordinator.domain.model.DeviceState;
import com.gridauthority.coordinator.infrastructure.hcs.HcsAnchorService;
import com.gridauthority.coordinator.infrastructure.hcs.HcsEvent;
import com.gridauthority.coordinator.infrastructure.http.DeviceCommandClient;
import com.gridauthority.coordinator.infrastructure.kms.KmsSigningService;
import com.gridauthority.coordinator.infrastructure.registry.DeviceRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceCommandDispatcher {

    private final DeviceRegistry deviceRegistry;
    private final DeviceCommandClient deviceCommandClient;
    private final KmsSigningService kmsSigningService;
    private final HcsAnchorService hcsAnchorService;

    public void dispatch(DeviceMetricsDTO metrics, DeviceCommand command) {

        if (command == DeviceCommand.KEEP_RUNNING) return;

        if (command == DeviceCommand.SHUTDOWN && metrics.state() == DeviceState.SHUTDOWN) {
            log.debug("[DISPATCH] Skipping redundant SHUTDOWN for device={}", metrics.deviceId());
            return;
        }
        if (command == DeviceCommand.RESTART && metrics.state() == DeviceState.ACTIVE) {
            log.debug("[DISPATCH] Skipping redundant RESTART for device={}", metrics.deviceId());
            return;
        }

        SignedCommandPayload signed = kmsSigningService.createSignedCommand(
                metrics.deviceId(), command.name());

        deviceRegistry.resolve(metrics.deviceId()).ifPresentOrElse(
                baseUrl -> {
                    deviceCommandClient.send(baseUrl, metrics.deviceId(), command);
                    anchorDecision(metrics, command, signed);
                },
                () -> log.warn("[DISPATCH] No URL registered for device={} — {} not delivered",
                        metrics.deviceId(), command)
        );
    }

    private void anchorDecision(DeviceMetricsDTO metrics, DeviceCommand command,
                                SignedCommandPayload signed) {
        String reason = switch (command) {
            case SHUTDOWN -> "CV exceeded threshold";
            case RESTART -> "stable cycles reached";
            default -> null;
        };
        Double cv = command == DeviceCommand.SHUTDOWN ? metrics.cv() : null;

        HcsEvent event = HcsEvent.decision(
                metrics.deviceId(),
                command.name(),
                reason,
                cv,
                sha256(signed.canonicalJson()),
                signed.keyId(),
                signed.signingAlgorithm(),
                signed.signatureBase64()
        );
        hcsAnchorService.anchorDecision(event);
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
}
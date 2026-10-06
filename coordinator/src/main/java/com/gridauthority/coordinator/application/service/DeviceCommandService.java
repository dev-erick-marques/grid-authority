package com.gridauthority.coordinator.application.service;

import com.gridauthority.coordinator.application.dto.DeviceMetricsDTO;
import com.gridauthority.coordinator.domain.model.DeviceCommand;
import com.gridauthority.coordinator.domain.model.DeviceState;
import com.gridauthority.coordinator.infrastructure.audit.AuditEventPublisher;
import com.gridauthority.coordinator.infrastructure.audit.AuditLogEntry;
import com.gridauthority.coordinator.infrastructure.hcs.AuthorityKeyPublisher;
import com.gridauthority.coordinator.infrastructure.hcs.HcsAnchorService;
import com.gridauthority.coordinator.infrastructure.hcs.HcsEvent;
import com.gridauthority.coordinator.infrastructure.hcs.HcsPayload;
import com.gridauthority.coordinator.infrastructure.kms.KmsSigningService;
import com.gridauthority.coordinator.infrastructure.kms.SigningResult;
import com.gridauthority.coordinator.infrastructure.registry.DeviceRegistry;
import com.gridauthority.coordinator.infrastructure.transport.CommandTransport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceCommandService {

    private final DeviceRegistry deviceRegistry;
    private final CommandTransport commandTransport;
    private final KmsSigningService kmsSigningService;
    private final HcsAnchorService hcsAnchorService;
    private final AuditEventPublisher auditEventPublisher;
    private final ObjectMapper objectMapper;
    private final AuthorityKeyPublisher authorityKeyPublisher;

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

        if (!authorityKeyPublisher.isKeyActive()) {
            log.warn("[DISPATCH] Command dropped — authority key activation window not yet elapsed.");
            return;
        }

        SigningResult result = kmsSigningService.issueCommand(metrics.deviceId(), command.name());

        auditEventPublisher.publish(
                AuditLogEntry.kmsSigned(metrics.deviceId(), command.name(), result.keyId())
        );

        deviceRegistry.resolve(metrics.deviceId()).ifPresentOrElse(
                baseUrl -> {
                    commandTransport.send(baseUrl, metrics.deviceId(), command, result.payload());
                    anchorDecision(metrics, command, result);
                },
                () -> log.warn("[DISPATCH] No URL registered for device={} — {} not delivered",
                        metrics.deviceId(), command)
        );
    }

    private void anchorDecision(DeviceMetricsDTO metrics, DeviceCommand command, SigningResult result) {
        String reason = switch (command) {
            case SHUTDOWN -> "CV exceeded threshold";
            case RESTART  -> "stable cycles reached";
            default       -> null;
        };

        HcsPayload payload = HcsPayload.builder()
                .eventType(HcsEvent.EventType.DECISION.name())
                .deviceId(metrics.deviceId())
                .action(command.name())
                .reason(reason)
                .cv(metrics.cv())
                .mean(metrics.mean())
                .std(metrics.std())
                .keyId(result.keyId())
                .signingAlgorithm(result.signingAlgorithm())
                .signatureBase64(result.payload().signatureBase64())
                .timestamp(result.context().issuedAt())
                .build();

        hcsAnchorService.anchorDecision(HcsEvent.of(payload, objectMapper));
    }
}
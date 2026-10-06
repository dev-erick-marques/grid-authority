package com.gridauthority.coordinator.application.service;

import com.gridauthority.coordinator.application.dto.DeviceMetricsDTO;
import com.gridauthority.coordinator.application.dto.SignedCommandPayload;
import com.gridauthority.coordinator.domain.model.DeviceCommand;
import com.gridauthority.coordinator.domain.model.DeviceState;
import com.gridauthority.coordinator.infrastructure.audit.AuditEventPublisher;
import com.gridauthority.coordinator.infrastructure.audit.AuditLogEntry;
import com.gridauthority.coordinator.infrastructure.registry.DeviceRegistry;
import com.gridauthority.coordinator.infrastructure.transport.CommandTransport;
import com.gridauthority.coordinator.signing.LocalSigningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceCommandService {

    private final DeviceRegistry deviceRegistry;
    private final CommandTransport commandTransport;
    private final LocalSigningService signingService;
    private final AuditEventPublisher auditEventPublisher;

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

        SignedCommandPayload payload = signingService.sign(metrics.deviceId(), command);

        auditEventPublisher.publish(AuditLogEntry.builder()
                .type("DECISION")
                .eventType("SIGNED_COMMAND")
                .deviceId(metrics.deviceId())
                .action(command.name())
                .ts(System.currentTimeMillis())
                .build());

        deviceRegistry.resolve(metrics.deviceId()).ifPresentOrElse(
                baseUrl -> commandTransport.send(baseUrl, metrics.deviceId(), command, payload),
                () -> log.warn("[DISPATCH] No URL registered for device={} — {} not delivered",
                        metrics.deviceId(), command)
        );
    }
}

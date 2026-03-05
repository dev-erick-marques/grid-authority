package com.gridauthority.coordinator.application.service;

import com.gridauthority.coordinator.application.dto.DeviceMetricsDTO;
import com.gridauthority.coordinator.domain.model.DeviceCommand;
import com.gridauthority.coordinator.infrastructure.http.DeviceCommandClient;
import com.gridauthority.coordinator.infrastructure.registry.DeviceRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceCommandDispatcher {


    private final DeviceRegistry deviceRegistry;
    private final DeviceCommandClient deviceCommandClient;

    public void dispatch(DeviceMetricsDTO metrics, DeviceCommand command) {

        if (command == DeviceCommand.KEEP_RUNNING) {
            return;
        }

        deviceRegistry.resolve(metrics.deviceId()).ifPresentOrElse(
                baseUrl -> deviceCommandClient.send(baseUrl, metrics.deviceId(), command),
                () -> log.warn("[DISPATCH] No URL registered for device={} — command {} not delivered",
                        metrics.deviceId(), command)
        );

        // TODO: anchor decision to Hedera HCS (requires HcsAnchorService)
        // hcsAnchorService.anchor(HcsEvent.decision(metrics));
    }

}
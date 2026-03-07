package com.gridauthority.coordinator.infrastructure.http;

import com.gridauthority.coordinator.application.dto.SignedCommandPayload;
import com.gridauthority.coordinator.domain.model.DeviceCommand;
import com.gridauthority.coordinator.infrastructure.kms.KmsSigningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceCommandClient {

    private static final String COMMANDS_PATH = "/api/command";

    private final RestTemplate restTemplate;
    private final KmsSigningService kmsSigningService;

    public void send(String deviceBaseUrl, String deviceId, DeviceCommand command) {
        SignedCommandPayload payload = kmsSigningService.sign(deviceId, command.name());
        String url = deviceBaseUrl + COMMANDS_PATH;

        try {
            restTemplate.postForLocation(url, payload);
            log.info("[HTTP] {} → device={} at {} (signed keyId={})",
                    command, deviceId, deviceBaseUrl, payload.keyId());
        } catch (RestClientException e) {
            log.error("[HTTP] Failed to send {} → device={} at {} — {}",
                    command, deviceId, deviceBaseUrl, e.getMessage());
        }
    }
}
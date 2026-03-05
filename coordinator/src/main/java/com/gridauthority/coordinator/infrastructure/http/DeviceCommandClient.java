package com.gridauthority.coordinator.infrastructure.http;

import com.gridauthority.coordinator.domain.model.DeviceCommand;
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

    public void send(String deviceBaseUrl, String deviceId, DeviceCommand command) {
        CommandPayload payload = new CommandPayload(deviceId, command);
        String url = deviceBaseUrl + COMMANDS_PATH;

        try {
            restTemplate.postForLocation(url, payload);
            log.info("[HTTP] {} → device={} at {}", command, deviceId, deviceBaseUrl);

        } catch (RestClientException e) {
            log.error("[HTTP] Failed to send {} → device={} at {} — {}",
                    command, deviceId, deviceBaseUrl, e.getMessage());
        }
    }

    private record CommandPayload(
            String deviceId,
            DeviceCommand command
    ) {}
}
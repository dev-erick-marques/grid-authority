package com.gridauthority.coordinator.infrastructure.http;

import com.gridauthority.coordinator.application.dto.SignedCommandPayload;
import com.gridauthority.coordinator.domain.model.DeviceCommand;
import com.gridauthority.coordinator.infrastructure.transport.CommandTransport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "transport.mode", havingValue = "http", matchIfMissing = true)
public class DeviceCommandClient implements CommandTransport {

    private static final String COMMANDS_PATH = "/api/command";

    private final RestTemplate restTemplate;

    @Override
    public void send(String deviceBaseUrl, String deviceId,
                     DeviceCommand command, SignedCommandPayload payload) {
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
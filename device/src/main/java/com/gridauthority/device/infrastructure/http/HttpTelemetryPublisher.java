package com.gridauthority.device.infrastructure.http;

import com.gridauthority.device.aplication.dto.DeviceTelemetryDTO;
import com.gridauthority.device.infrastructure.transport.TelemetryTransport;
import com.gridauthority.device.infrastructure.config.CoordinatorProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "transport.mode", havingValue = "http", matchIfMissing = true)
public class HttpTelemetryPublisher implements TelemetryTransport {

    private final CoordinatorProperties coordinatorProperties;
    private final RestClient restClient;

    @Override
    public void dispatch(DeviceTelemetryDTO telemetry) {
        restClient.post()
                .uri(coordinatorProperties.getUrl() + "/api/devices/telemetry")
                .contentType(MediaType.APPLICATION_JSON)
                .body(telemetry)
                .retrieve()
                .toBodilessEntity();
        log.debug("[HTTP] Telemetry dispatched deviceId={} voltage={}",
                telemetry.deviceId(), telemetry.voltage());
    }
}
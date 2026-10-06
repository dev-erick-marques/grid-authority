package com.gridauthority.coordinator.infrastructure.mqtt;

import com.gridauthority.coordinator.application.dto.DeviceTelemetryDTO;
import com.gridauthority.coordinator.application.service.TelemetryIngestionService;
import com.gridauthority.coordinator.infrastructure.config.MqttProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@RequiredArgsConstructor
public class MqttTelemetrySubscriber {

    private final MqttClient mqttClient;
    private final MqttProperties mqttProperties;
    private final TelemetryIngestionService telemetryIngestionService;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void subscribe() throws MqttException {
        String wildcard = mqttProperties.telemetryTopicWildcard();

        mqttClient.subscribe(wildcard, mqttProperties.getQos(), (topic, message) -> {
            try {
                DeviceTelemetryDTO telemetry =
                        objectMapper.readValue(message.getPayload(), DeviceTelemetryDTO.class);
                log.debug("[MQTT] Telemetry received topic={} deviceId={}", topic, telemetry.deviceId());
                telemetryIngestionService.ingest(telemetry);
            } catch (Exception e) {
                log.error("[MQTT] Failed to parse telemetry from topic={} — {}", topic, e.getMessage());
            }
        });

        log.info("[MQTT] Coordinator subscribed to telemetry topic={}", wildcard);
    }
}
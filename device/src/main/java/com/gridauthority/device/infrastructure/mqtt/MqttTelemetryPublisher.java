package com.gridauthority.device.infrastructure.mqtt;

import com.gridauthority.device.aplication.dto.DeviceTelemetryDTO;
import com.gridauthority.device.infrastructure.config.MqttProperties;
import com.gridauthority.device.infrastructure.transport.TelemetryTransport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@RequiredArgsConstructor
public class MqttTelemetryPublisher implements TelemetryTransport {

    private final MqttClient mqttClient;
    private final MqttProperties mqttProperties;
    private final ObjectMapper objectMapper;

    @Override
    public void dispatch(DeviceTelemetryDTO telemetry) {
        String topic = mqttProperties.telemetryTopic(telemetry.deviceId());
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(telemetry);
            MqttMessage msg = new MqttMessage(bytes);
            msg.setQos(mqttProperties.getQos());
            msg.setRetained(false);
            mqttClient.publish(topic, msg);
            log.debug("[MQTT] Telemetry published topic={} voltage={}", topic, telemetry.voltage());
        } catch (MqttException e) {
            log.error("[MQTT] Failed to publish telemetry topic={} — {}", topic, e.getMessage());
            throw new RuntimeException("MQTT telemetry publish failed", e);
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed for telemetry", e);
        }
    }
}
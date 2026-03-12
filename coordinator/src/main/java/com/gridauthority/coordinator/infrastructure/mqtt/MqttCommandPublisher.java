package com.gridauthority.coordinator.infrastructure.mqtt;

import com.gridauthority.coordinator.application.dto.SignedCommandPayload;
import com.gridauthority.coordinator.domain.model.DeviceCommand;
import com.gridauthority.coordinator.infrastructure.config.MqttProperties;
import com.gridauthority.coordinator.infrastructure.transport.CommandTransport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@RequiredArgsConstructor
public class MqttCommandPublisher implements CommandTransport {

    private final MqttClient mqttClient;
    private final MqttProperties mqttProperties;
    private final ObjectMapper objectMapper;

    @Override
    public void send(String deviceBaseUrl, String deviceId,
                     DeviceCommand command, SignedCommandPayload payload) {
        String topic = mqttProperties.commandTopic(deviceId);
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(payload);
            MqttMessage msg = new MqttMessage(bytes);
            msg.setQos(mqttProperties.getQos());
            msg.setRetained(false);
            mqttClient.publish(topic, msg);
            log.info("[MQTT] {} → device={} topic={} (signed keyId={})",
                    command, deviceId, topic, payload.keyId());
        } catch (MqttException e) {
            throw new RuntimeException("MQTT publish failed for command " + command, e);
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed for command " + command, e);
        }
    }
}
package com.gridauthority.coordinator.infrastructure.mqtt;

import com.gridauthority.coordinator.application.dto.SignedCommandPayload;
import com.gridauthority.coordinator.infrastructure.config.MqttProperties;
import com.gridauthority.coordinator.infrastructure.transport.SurgeTransport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@RequiredArgsConstructor
public class MqttSurgePublisher implements SurgeTransport {

    private final MqttClient mqttClient;
    private final MqttProperties mqttProperties;
    private final ObjectMapper objectMapper;

    @Override
    public void send(String deviceBaseUrl, String deviceId,
                     String action, SignedCommandPayload payload) {
        String topic = mqttProperties.surgeTopic(deviceId);
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(payload);
            MqttMessage msg = new MqttMessage(bytes);
            msg.setQos(mqttProperties.getQos());
            msg.setRetained(false);
            mqttClient.publish(topic, msg);
            log.info("[MQTT-SURGE] {} → device={} key={}", action, deviceId, shortKeyId(payload.keyId()));
        } catch (MqttException e) {
            throw new RuntimeException("MQTT surge publish failed for action " + action, e);
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed for surge action " + action, e);
        }
    }

    private static String shortKeyId(String keyId) {
        if (keyId == null) return "none";
        int slash = keyId.lastIndexOf('/');
        return slash >= 0 ? keyId.substring(slash + 1) : keyId;
    }
}
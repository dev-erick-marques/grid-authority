package com.gridauthority.device.infrastructure.mqtt;

import com.gridauthority.device.aplication.dto.SignedCommandDTO;
import com.gridauthority.device.aplication.service.CommandVerificationService;
import com.gridauthority.device.aplication.service.DeviceStateService;
import com.gridauthority.device.aplication.service.SurgeModeService;
import com.gridauthority.device.domain.model.DeviceCommand;
import com.gridauthority.device.domain.model.SurgeAction;
import com.gridauthority.device.infrastructure.config.DeviceSimulationProperties;
import com.gridauthority.device.infrastructure.config.MqttProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@RequiredArgsConstructor
public class MqttCommandSubscriber {

    private final MqttClient mqttClient;
    private final MqttProperties mqttProperties;
    private final DeviceSimulationProperties simulationProperties;
    private final CommandVerificationService verificationService;
    private final DeviceStateService deviceStateService;
    private final SurgeModeService surgeModeService;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void subscribe() throws MqttException {
        String deviceId = simulationProperties.getId();

        subscribeCommands(deviceId);
        subscribeSurge(deviceId);

        log.info("[MQTT] Device {} subscribed to command and surge topics", deviceId);
    }

    private void subscribeCommands(String deviceId) throws MqttException {
        String topic = mqttProperties.commandTopic(deviceId);
        mqttClient.subscribe(topic, mqttProperties.getQos(), (t, message) -> {
            try {
                SignedCommandDTO signed = objectMapper.readValue(
                        message.getPayload(), SignedCommandDTO.class);

                verificationService.verify(
                        signed.signatureBase64(), signed.canonicalJson(), signed.issuedAt());

                DeviceCommand command = DeviceCommand.valueOf(signed.action());
                log.info("[MQTT] Verified {} for device={}", command, deviceId);

                switch (command) {
                    case SHUTDOWN -> deviceStateService.shutdown();
                    case RESTART  -> deviceStateService.restart();
                }
            } catch (IllegalArgumentException e) {
                log.error("[MQTT] Unknown command in message from topic={} — {}", t, e.getMessage());
            } catch (Exception e) {
                log.error("[MQTT] Failed to process command from topic={} — {}", t, e.getMessage());
            }
        });
    }

    private void subscribeSurge(String deviceId) throws MqttException {
        String topic = mqttProperties.surgeTopic(deviceId);
        mqttClient.subscribe(topic, mqttProperties.getQos(), (t, message) -> {
            try {
                SignedCommandDTO signed = objectMapper.readValue(
                        message.getPayload(), SignedCommandDTO.class);

                verificationService.verify(
                        signed.signatureBase64(), signed.canonicalJson(), signed.issuedAt());

                SurgeAction surgeAction = SurgeAction.valueOf(signed.action());
                log.info("[MQTT-SURGE] Verified {} for device={}", surgeAction, deviceId);

                switch (surgeAction) {
                    case SURGE_START       -> surgeModeService.forceSurge();
                    case SURGE_STOP        -> surgeModeService.forceNormal();
                    case SURGE_CYCLE_START -> surgeModeService.startAutoCycle();
                    case SURGE_CYCLE_STOP  -> surgeModeService.stopAutoCycle();
                }
            } catch (IllegalArgumentException e) {
                log.error("[MQTT-SURGE] Unknown action from topic={} — {}", t, e.getMessage());
            } catch (Exception e) {
                log.error("[MQTT-SURGE] Failed to process surge from topic={} — {}", t, e.getMessage());
            }
        });
    }
}
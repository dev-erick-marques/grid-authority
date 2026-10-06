package com.gridauthority.device.infrastructure.config;

import com.gridauthority.device.aplication.service.CommandVerificationService;
import com.gridauthority.device.aplication.service.DeviceStateService;
import com.gridauthority.device.aplication.service.SurgeModeService;
import com.gridauthority.device.infrastructure.mqtt.MqttCommandSubscriber;
import com.gridauthority.device.infrastructure.mqtt.MqttTelemetryPublisher;
import com.gridauthority.device.infrastructure.mqtt.TrustAllSslFactory;
import com.gridauthority.device.infrastructure.transport.TelemetryTransport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "transport.mode", havingValue = "mqtt")
@EnableConfigurationProperties(MqttProperties.class)
public class MqttConfig {

    private final MqttProperties mqttProperties;
    private final DeviceSimulationProperties simulationProperties;

    @Bean(destroyMethod = "disconnect")
    public MqttClient mqttClient() throws MqttException {
        String clientId = mqttProperties.clientId(simulationProperties.getId());

        MqttClient client = new MqttClient(
                mqttProperties.getBrokerUrl(),
                clientId,
                new MemoryPersistence()
        );

        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setCleanSession(true);
        opts.setAutomaticReconnect(true);
        opts.setConnectionTimeout(10);
        opts.setKeepAliveInterval(30);

        if (mqttProperties.getBrokerUrl().startsWith("ssl://")) {
            opts.setSocketFactory(TrustAllSslFactory.create());
            log.info("[MQTT] TLS enabled (trust-all) for broker={}", mqttProperties.getBrokerUrl());
        }

        client.connect(opts);
        log.info("[MQTT] Device {} connected to broker={} clientId={}",
                simulationProperties.getId(), mqttProperties.getBrokerUrl(), clientId);

        return client;
    }

    @Bean
    public TelemetryTransport mqttTelemetryPublisher(
            MqttClient mqttClient,
            ObjectMapper objectMapper
    ) {
        return new MqttTelemetryPublisher(mqttClient, mqttProperties, objectMapper);
    }

    @Bean
    public MqttCommandSubscriber mqttCommandSubscriber(
            MqttClient mqttClient,
            ObjectMapper objectMapper,
            CommandVerificationService verificationService,
            DeviceStateService deviceStateService,
            SurgeModeService surgeModeService
    ) {

        return new MqttCommandSubscriber(
                mqttClient, mqttProperties, simulationProperties,
                verificationService, deviceStateService, surgeModeService, objectMapper);
    }
}
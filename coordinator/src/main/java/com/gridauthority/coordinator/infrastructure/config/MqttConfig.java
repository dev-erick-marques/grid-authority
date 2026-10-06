package com.gridauthority.coordinator.infrastructure.config;

import com.gridauthority.coordinator.application.service.TelemetryIngestionService;
import com.gridauthority.coordinator.infrastructure.mqtt.MqttCommandPublisher;
import com.gridauthority.coordinator.infrastructure.mqtt.MqttSurgePublisher;
import com.gridauthority.coordinator.infrastructure.mqtt.MqttTelemetrySubscriber;
import com.gridauthority.coordinator.infrastructure.mqtt.TrustAllSslFactory;
import com.gridauthority.coordinator.infrastructure.transport.CommandTransport;
import com.gridauthority.coordinator.infrastructure.transport.SurgeTransport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "transport.mode", havingValue = "mqtt")
public class MqttConfig {

    private final MqttProperties mqttProperties;

    @Bean(destroyMethod = "disconnect")
    public MqttClient mqttClient() throws MqttException {
        MqttClient client = new MqttClient(
                mqttProperties.getBrokerUrl(),
                mqttProperties.getClientId(),
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
        log.info("[MQTT] Coordinator connected to broker={} clientId={}",
                mqttProperties.getBrokerUrl(), mqttProperties.getClientId());

        return client;
    }

    @Bean
    public CommandTransport mqttCommandPublisher(MqttClient mqttClient,
                                                 ObjectMapper objectMapper) {
        return new MqttCommandPublisher(mqttClient, mqttProperties, objectMapper);
    }

    @Bean
    public SurgeTransport mqttSurgePublisher(MqttClient mqttClient,
                                             ObjectMapper objectMapper) {
        return new MqttSurgePublisher(mqttClient, mqttProperties, objectMapper);
    }

    @Bean
    public MqttTelemetrySubscriber mqttTelemetrySubscriber(MqttClient mqttClient,
                                                           TelemetryIngestionService telemetryIngestionService,
                                                           ObjectMapper objectMapper) {
        return new MqttTelemetrySubscriber(mqttClient, mqttProperties,
                telemetryIngestionService, objectMapper);
    }
}
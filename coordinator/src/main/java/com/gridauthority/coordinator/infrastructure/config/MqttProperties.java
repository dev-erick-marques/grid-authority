package com.gridauthority.coordinator.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "mqtt")
public class MqttProperties {

    private String brokerUrl = "ssl://hivemq:8883";
    private String clientId = "coordinator";
    private int qos = 1;
    private String topicPrefix = "gridauthority";

    public String telemetryTopic(String deviceId) {
        return topicPrefix + "/telemetry/" + deviceId;
    }

    public String telemetryTopicWildcard() {
        return topicPrefix + "/telemetry/#";
    }

    public String commandTopic(String deviceId) {
        return topicPrefix + "/commands/" + deviceId;
    }

    public String surgeTopic(String deviceId) {
        return topicPrefix + "/surge/" + deviceId;
    }
}
package com.gridauthority.device;

import com.gridauthority.device.infrastructure.config.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({DeviceSimulationProperties.class, DeviceNetworkProperties.class, CoordinatorProperties.class, HcsDeviceProperties.class, MqttProperties.class})
public class DeviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DeviceApplication.class, args);
	}

}

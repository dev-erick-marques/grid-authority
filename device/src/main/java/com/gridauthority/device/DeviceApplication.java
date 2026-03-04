package com.gridauthority.device;

import com.gridauthority.device.infrastructure.config.CoordinatorProperties;
import com.gridauthority.device.infrastructure.config.DeviceNetworkProperties;
import com.gridauthority.device.infrastructure.config.DeviceSimulationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({DeviceSimulationProperties.class, DeviceNetworkProperties.class, CoordinatorProperties.class})
public class DeviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DeviceApplication.class, args);
	}

}

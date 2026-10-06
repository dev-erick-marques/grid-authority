package com.gridauthority.device.infrastructure.config;

import com.gridauthority.device.domain.model.GridVoltageModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;

@Configuration
public class GridVoltageModelConfig {

    @Bean
    public GridVoltageModel gridVoltageModel(DeviceSimulationProperties properties) {
        DeviceSimulationProperties.Voltage v = properties.getVoltage();
        return new GridVoltageModel(
                new GridVoltageModel.Params(v.getBase(), v.getWanderStd(), v.getWanderTimeConstantSeconds(),
                        v.getLoadSwing(), v.getLoadPeriodSeconds(), v.getNoiseStd(), v.getResolution()),
                new Random());
    }
}

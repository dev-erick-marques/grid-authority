package com.gridauthority.device.aplication.service;

import com.gridauthority.device.domain.exception.DeviceAlreadyActiveException;
import com.gridauthority.device.domain.exception.DeviceAlreadyShutdownException;
import com.gridauthority.device.domain.model.DeviceState;
import com.gridauthority.device.infrastructure.config.DeviceSimulationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceStateService {

    private final DeviceSimulationProperties properties;
    private final AtomicReference<DeviceState> state = new AtomicReference<>(DeviceState.ACTIVE);

    public DeviceState current() {
        return state.get();
    }

    public DeviceState shutdown() {
        if (state.get() == DeviceState.SHUTDOWN) {
            throw new DeviceAlreadyShutdownException(properties.getId());
        }
        state.set(DeviceState.SHUTDOWN);
        log.warn("[DEVICE] {} → SHUTDOWN", properties.getId());
        return state.get();
    }

    public DeviceState restart() {
        if (state.get() == DeviceState.ACTIVE) {
            throw new DeviceAlreadyActiveException(properties.getId());
        }
        state.set(DeviceState.ACTIVE);
        log.info("[DEVICE] {} → ACTIVE", properties.getId());
        return state.get();
    }
}
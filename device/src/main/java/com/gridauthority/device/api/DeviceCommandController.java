package com.gridauthority.device.api;

import com.gridauthority.device.aplication.dto.DeviceCommandDTO;
import com.gridauthority.device.aplication.service.DeviceStateService;
import com.gridauthority.device.domain.model.DeviceState;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/device")
public class DeviceCommandController {

    private final DeviceStateService deviceStateService;

    @GetMapping("/state")
    public DeviceState getState() {
        return deviceStateService.current();
    }

    @PostMapping("/command")
    public DeviceState command(@RequestBody DeviceCommandDTO command) {
        return switch (command.command()) {
            case SHUTDOWN -> deviceStateService.shutdown();
            case RESTART  -> deviceStateService.restart();
        };
    }
}
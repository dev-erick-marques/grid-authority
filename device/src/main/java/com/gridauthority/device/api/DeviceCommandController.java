package com.gridauthority.device.api;

import com.gridauthority.device.aplication.dto.CommandSigningContext;
import com.gridauthority.device.aplication.dto.SignedCommandDTO;
import com.gridauthority.device.aplication.service.CommandVerificationService;
import com.gridauthority.device.aplication.service.DeviceStateService;
import com.gridauthority.device.domain.exception.UnknownCommandException;
import com.gridauthority.device.domain.model.DeviceCommand;
import com.gridauthority.device.domain.model.DeviceState;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Validated
public class DeviceCommandController {

    private final DeviceStateService deviceStateService;
    private final CommandVerificationService verificationService;

    @GetMapping("/state")
    public DeviceState getState() {
        return deviceStateService.current();
    }

    @PostMapping("/command")
    public DeviceState command(@RequestBody @Valid SignedCommandDTO signed) {
        CommandSigningContext context = verificationService.verify(
                signed.signatureBase64(), signed.canonicalJson()
        );

        DeviceCommand command;
        try {
            command = DeviceCommand.valueOf(context.action());
        } catch (IllegalArgumentException e) {
            throw new UnknownCommandException(context.action());
        }

        log.info("[COMMAND] Verified {} for device={}", command, context.deviceId());
        return switch (command) {
            case SHUTDOWN -> deviceStateService.shutdown();
            case RESTART  -> deviceStateService.restart();
        };
    }
}
package com.gridauthority.device.api;

import com.gridauthority.device.aplication.dto.SignedCommandDTO;
import com.gridauthority.device.aplication.service.CommandVerificationService;
import com.gridauthority.device.aplication.service.DeviceStateService;
import com.gridauthority.device.domain.model.DeviceCommand;
import com.gridauthority.device.domain.model.DeviceState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class DeviceCommandController {

    private final DeviceStateService deviceStateService;
    private final CommandVerificationService verificationService;

    @GetMapping("/state")
    public DeviceState getState() {
        return deviceStateService.current();
    }

    @PostMapping("/command")
    public DeviceState command(@RequestBody SignedCommandDTO signed) {
        verifyOrReject(signed);

        DeviceCommand command;
        try {
            command = DeviceCommand.valueOf(signed.action());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown device command: " + signed.action());
        }

        log.info("[COMMAND] Verified {} for device={}", command, signed.deviceId());
        return switch (command) {
            case SHUTDOWN -> deviceStateService.shutdown();
            case RESTART  -> deviceStateService.restart();
        };
    }

    private void verifyOrReject(SignedCommandDTO signed) {
        boolean valid = verificationService.verify(
                signed.signatureBase64(),
                signed.canonicalJson(),
                signed.issuedAt()
        );
        if (!valid) {
            log.warn("[COMMAND] Rejected unsigned/invalid command action={} device={}",
                    signed.action(), signed.deviceId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Command rejected: invalid or missing KMS signature");
        }
    }
}
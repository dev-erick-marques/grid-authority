package com.gridauthority.device.api;

import com.gridauthority.device.aplication.dto.SignedCommandDTO;
import com.gridauthority.device.aplication.service.CommandVerificationService;
import com.gridauthority.device.aplication.service.SurgeModeService;
import com.gridauthority.device.domain.model.SurgeAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class SurgeSimulationController {

    private final SurgeModeService surgeModeService;
    private final CommandVerificationService verificationService;

    public record SurgeResponse(String status, Boolean surgeActive) {}

    @PostMapping("/surge/signed")
    public ResponseEntity<SurgeResponse> signedSurge(@RequestBody SignedCommandDTO signed) {
        boolean valid = verificationService.verify(
                signed.signatureBase64(), signed.canonicalJson(), signed.issuedAt()
        );
        if (!valid) {
            log.warn("[SURGE] Rejected unsigned/invalid surge command action={} device={}",
                    signed.action(), signed.deviceId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Surge command rejected: invalid or missing KMS signature");
        }

        log.info("[SURGE] Verified {} for device={}", signed.action(), signed.deviceId());

        SurgeAction surgeAction;
        try {
            surgeAction = SurgeAction.valueOf(signed.action());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown surge action: " + signed.action());
        }

        return switch (surgeAction) {
            case SURGE_START -> { surgeModeService.forceSurge();
                yield ResponseEntity.ok(new SurgeResponse("surge activated", true));
            }
            case SURGE_STOP -> { surgeModeService.forceNormal();
                yield ResponseEntity.ok(new SurgeResponse("surge deactivated", false));
            }
            case SURGE_CYCLE_START -> { surgeModeService.startAutoCycle();
                yield ResponseEntity.ok(new SurgeResponse("auto cycle started", surgeModeService.isSurgeActive()));
            }
            case SURGE_CYCLE_STOP -> { surgeModeService.stopAutoCycle();
                yield ResponseEntity.ok(new SurgeResponse("auto cycle stopped", surgeModeService.isSurgeActive()));
            }
        };
    }
}
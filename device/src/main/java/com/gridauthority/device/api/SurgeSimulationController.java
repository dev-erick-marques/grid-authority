package com.gridauthority.device.api;

import com.gridauthority.device.aplication.dto.CommandSigningContext;
import com.gridauthority.device.aplication.dto.SignedCommandDTO;
import com.gridauthority.device.aplication.service.CommandVerificationService;
import com.gridauthority.device.aplication.service.SurgeModeService;
import com.gridauthority.device.domain.exception.UnknownCommandException;
import com.gridauthority.device.domain.model.SurgeAction;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Validated
public class SurgeSimulationController {

    private final SurgeModeService surgeModeService;
    private final CommandVerificationService verificationService;

    public record SurgeResponse(String status, Boolean surgeActive) {}

    @PostMapping("/surge/signed")
    public ResponseEntity<SurgeResponse> signedSurge(@RequestBody @Valid SignedCommandDTO signed) {
        CommandSigningContext context = verificationService.verify(
                signed.signatureBase64(), signed.canonicalJson()
        );

        log.info("[SURGE] Verified {} for device={}", context.action(), context.deviceId());

        SurgeAction surgeAction;
        try {
            surgeAction = SurgeAction.valueOf(context.action());
        } catch (IllegalArgumentException e) {
            throw new UnknownCommandException(context.action());
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
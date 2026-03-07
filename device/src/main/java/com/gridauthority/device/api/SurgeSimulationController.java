package com.gridauthority.device.api;

import com.gridauthority.device.aplication.service.SurgeModeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class SurgeSimulationController {

    private final SurgeModeService surgeModeService;
    public record SurgeResponse(String status, Boolean surgeActive) {}

    @GetMapping("/surge")
    public ResponseEntity<SurgeResponse> status() {
        return ResponseEntity.ok(
                new SurgeResponse("status retrieved", surgeModeService.isSurgeActive())
        );
    }

    @PostMapping("/surge/start")
    public ResponseEntity<SurgeResponse> startSurge() {
        surgeModeService.forceSurge();
        return ResponseEntity.ok(
                new SurgeResponse("surge activated", true)
        );
    }

    @PostMapping("/surge/stop")
    public ResponseEntity<SurgeResponse> stopSurge() {
        surgeModeService.forceNormal();
        return ResponseEntity.ok(
                new SurgeResponse("surge deactivated", false)
        );
    }

    @PostMapping("/surge/cycle/start")
    public ResponseEntity<SurgeResponse> startCycle() {
        surgeModeService.startAutoCycle();
        return ResponseEntity.ok(
                new SurgeResponse("auto cycle started", surgeModeService.isSurgeActive())
        );
    }

    @PostMapping("/surge/cycle/stop")
    public ResponseEntity<SurgeResponse> stopCycle() {
        surgeModeService.stopAutoCycle();
        return ResponseEntity.ok(
                new SurgeResponse("auto cycle stopped", surgeModeService.isSurgeActive())
        );
    }

}
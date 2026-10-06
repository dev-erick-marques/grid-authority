package com.gridauthority.coordinator.api;

import com.gridauthority.coordinator.application.service.CoordinatorSurgeService;
import com.gridauthority.coordinator.domain.model.DeviceSurgeState;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/devices/{deviceId}/surge")
@Validated
public class SurgeController {

    private final CoordinatorSurgeService coordinatorSurgeService;

    public record SurgeStatusResponse(String deviceId, DeviceSurgeState surgeState) {}
    public record SurgeActionResponse(String deviceId, String action, DeviceSurgeState surgeState) {}
    public record ErrorResponse(String error, String detail) {}

    @GetMapping
    public ResponseEntity<SurgeStatusResponse> getStatus(@PathVariable @NotBlank String deviceId) {
        DeviceSurgeState state = coordinatorSurgeService.getSurgeState(deviceId);
        return ResponseEntity.ok(new SurgeStatusResponse(deviceId, state));
    }

    @PostMapping("/start")
    public ResponseEntity<?> startSurge(@PathVariable @NotBlank String deviceId) {
        return executeAction(deviceId, "SURGE_START", () -> coordinatorSurgeService.startSurge(deviceId));
    }

    @PostMapping("/stop")
    public ResponseEntity<?> stopSurge(@PathVariable @NotBlank String deviceId) {
        return executeAction(deviceId, "SURGE_STOP", () -> coordinatorSurgeService.stopSurge(deviceId));
    }

    @PostMapping("/cycle/start")
    public ResponseEntity<?> startCycle(@PathVariable @NotBlank String deviceId) {
        return executeAction(deviceId, "SURGE_CYCLE_START", () -> coordinatorSurgeService.startCycle(deviceId));
    }

    @PostMapping("/cycle/stop")
    public ResponseEntity<?> stopCycle(@PathVariable @NotBlank String deviceId) {
        return executeAction(deviceId, "SURGE_CYCLE_STOP", () -> coordinatorSurgeService.stopCycle(deviceId));
    }

    private ResponseEntity<?> executeAction(String deviceId, String action, Runnable svc) {
        try {
            svc.run();
            DeviceSurgeState state = coordinatorSurgeService.getSurgeState(deviceId);
            return ResponseEntity.ok(new SurgeActionResponse(deviceId, action, state));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(404)
                    .body(new ErrorResponse("DEVICE_NOT_FOUND", e.getMessage()));
        } catch (RestClientException e) {
            return ResponseEntity.status(502)
                    .body(new ErrorResponse("DEVICE_UNREACHABLE", e.getMessage()));
        }
    }
}
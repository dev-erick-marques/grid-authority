package com.gridauthority.coordinator.api;

import com.gridauthority.coordinator.application.dto.DeviceMetricsDTO;
import com.gridauthority.coordinator.application.dto.DeviceTelemetryDTO;
import com.gridauthority.coordinator.application.service.TelemetryIngestionService;
import com.gridauthority.coordinator.infrastructure.repository.MetricsHistoryRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Validated
public class MetricsController {

    private final MetricsHistoryRepository metricsHistoryRepository;
    private final TelemetryIngestionService telemetryIngestionService;

    @PostMapping("/devices/telemetry")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void ingest(@RequestBody @Valid DeviceTelemetryDTO telemetry) {
        telemetryIngestionService.ingest(telemetry);
    }

    @GetMapping("/devices/metrics")
    public Map<String, List<DeviceMetricsDTO>> getAll() {
        return metricsHistoryRepository.getAll();
    }

    @GetMapping("/devices/{deviceId}/metrics")
    public List<DeviceMetricsDTO> getByDevice(@PathVariable @NotBlank String deviceId) {
        return metricsHistoryRepository.getByDevice(deviceId);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        telemetryIngestionService.registerEmitter(emitter);
        return emitter;
    }
}
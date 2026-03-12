package com.gridauthority.coordinator.api;

import com.gridauthority.coordinator.infrastructure.audit.AuditEventPublisher;
import com.gridauthority.coordinator.infrastructure.config.HcsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditEventPublisher auditEventPublisher;
    private final HcsProperties hcsProperties;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        auditEventPublisher.registerEmitter(emitter);
        return emitter;
    }

    @GetMapping("/topics")
    public HcsTopicsDTO topics() {
        return new HcsTopicsDTO(
                hcsProperties.getNetwork(),
                hcsProperties.getPublicKeyTopicId(),
                hcsProperties.getDecisionTopicId(),
                hcsProperties.getSurgeTopicId()
        );
    }

    public record HcsTopicsDTO(
            String network,
            String publicKeyTopicId,
            String decisionTopicId,
            String surgeTopicId
    ) {}
}
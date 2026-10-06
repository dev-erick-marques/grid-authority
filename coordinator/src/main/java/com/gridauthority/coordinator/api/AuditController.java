package com.gridauthority.coordinator.api;
import com.gridauthority.coordinator.infrastructure.audit.AuditEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
@RestController @RequiredArgsConstructor @RequestMapping("/api/audit")
public class AuditController {
 private final AuditEventPublisher publisher;
 @GetMapping(value="/stream",produces=MediaType.TEXT_EVENT_STREAM_VALUE)
 public SseEmitter stream(){SseEmitter e=new SseEmitter(Long.MAX_VALUE);publisher.registerEmitter(e);return e;}
}

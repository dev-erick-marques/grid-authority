package com.gridauthority.coordinator.application.service;
import com.gridauthority.coordinator.application.dto.DeviceMetricsDTO;
import com.gridauthority.coordinator.application.dto.SignedCommandPayload;
import com.gridauthority.coordinator.domain.model.DeviceCommand;
import com.gridauthority.coordinator.infrastructure.audit.AuditEventPublisher;
import com.gridauthority.coordinator.infrastructure.audit.AuditLogEntry;
import com.gridauthority.coordinator.infrastructure.registry.DeviceRegistry;
import com.gridauthority.coordinator.governance.ActionExecutionGuard;
import com.gridauthority.coordinator.governance.PolicyRepository;
import com.gridauthority.coordinator.infrastructure.transport.CommandTransport;
import com.gridauthority.coordinator.signing.LocalSigningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
@Service @RequiredArgsConstructor @Slf4j
public class DeviceCommandService {
 private final DeviceRegistry deviceRegistry; private final CommandTransport commandTransport;
 private final LocalSigningService signing; private final AuditEventPublisher audit;
 private final ActionExecutionGuard executionGuard; private final PolicyRepository policies;
 public void dispatch(DeviceMetricsDTO metrics,DeviceCommand command){
  if(command==DeviceCommand.OBSERVE||command==DeviceCommand.WARN)return;
  var policy = policies.getActive();
  deviceRegistry.resolve(metrics.deviceId()).ifPresentOrElse(baseUrl->{
   if(!executionGuard.allow(metrics.deviceId(),command,policy,System.currentTimeMillis())) {
    log.debug("[DISPATCH] Governance guard blocked action={} device={}",command,metrics.deviceId());
    return;
   }
   SignedCommandPayload payload=signing.sign(metrics.deviceId(),command);
   String commandHash=sha(payload.canonicalJson());
   commandTransport.send(baseUrl,metrics.deviceId(),command,payload);
   audit.publish(AuditLogEntry.builder().type("DECISION").eventType("SIGNED_COMMAND")
    .deviceId(metrics.deviceId()).action(command.name()).policyVersion(metrics.policyVersion())
    .riskScore(metrics.riskScore()).confidence(metrics.confidence()).timeToThreshold(metrics.timeToThreshold())
    .observabilityScore(metrics.observabilityScore()).metricsHash(sha(metrics.toString()))
    .commandHash(commandHash).ts(System.currentTimeMillis()).build());
  },()->log.warn("[DISPATCH] Device not registered: {}",metrics.deviceId()));
 }
 private String sha(String s){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}

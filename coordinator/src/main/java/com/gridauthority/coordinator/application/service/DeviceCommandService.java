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
 private final java.util.concurrent.ConcurrentHashMap<String,Long> lastWarningAuditMs=new java.util.concurrent.ConcurrentHashMap<>();
 private final java.util.concurrent.ConcurrentHashMap<String,Long> lastBlockedAuditMs=new java.util.concurrent.ConcurrentHashMap<>();
 /** @return true only if a signed command was actually sent to the device */
 public boolean dispatch(DeviceMetricsDTO metrics,DeviceCommand command){
  if(command==DeviceCommand.OBSERVE)return false;
  var policy = policies.getActive();
  if(command==DeviceCommand.WARN){ auditWarning(metrics,policy.cooldownSeconds()*1000L); return false; }
  var baseUrl=deviceRegistry.resolve(metrics.deviceId());
  if(baseUrl.isEmpty()){ log.warn("[DISPATCH] Device not registered: {}",metrics.deviceId()); return false; }
  var verdict=executionGuard.evaluate(metrics.deviceId(),command,policy,System.currentTimeMillis());
  if(verdict!=ActionExecutionGuard.Result.ALLOWED){
   if(verdict==ActionExecutionGuard.Result.BLOCKED_HOURLY_BUDGET) auditBlocked(metrics,command,verdict,policy.cooldownSeconds()*1000L);
   else log.debug("[DISPATCH] Cooldown active: action={} device={}",command,metrics.deviceId());
   return false;
  }
  {
   SignedCommandPayload payload=signing.sign(metrics.deviceId(),command);
   String commandHash=sha(payload.canonicalJson());
   commandTransport.send(baseUrl.get(),metrics.deviceId(),command,payload);
   audit.publish(AuditLogEntry.builder().type("DECISION").eventType("SIGNED_COMMAND")
    .deviceId(metrics.deviceId()).action(command.name()).policyVersion(metrics.policyVersion())
    .riskScore(metrics.riskScore()).confidence(metrics.confidence()).timeToThreshold(metrics.timeToThreshold())
    .observabilityScore(metrics.observabilityScore()).metricsHash(sha(metrics.toString()))
    .commandHash(commandHash).ts(System.currentTimeMillis()).build());
   return true;
  }
 }
 /** A blocked action must be visible: budget exhaustion in particular is an operational alarm. Throttled per device/action. */
 private void auditBlocked(DeviceMetricsDTO metrics,DeviceCommand command,ActionExecutionGuard.Result verdict,long minIntervalMs){
  long now=System.currentTimeMillis(); String key=metrics.deviceId()+"|"+command+"|"+verdict;
  Long last=lastBlockedAuditMs.get(key);
  if(last!=null&&now-last<minIntervalMs)return;
  lastBlockedAuditMs.put(key,now);
  log.warn("[DISPATCH] Hourly budget exhausted: action={} device={}",command,metrics.deviceId());
  audit.publish(AuditLogEntry.builder().type("DECISION").eventType("ACTION_BLOCKED_"+verdict.name())
   .deviceId(metrics.deviceId()).action(command.name()).policyVersion(metrics.policyVersion())
   .riskScore(metrics.riskScore()).confidence(metrics.confidence()).timeToThreshold(metrics.timeToThreshold())
   .observabilityScore(metrics.observabilityScore()).metricsHash(sha(metrics.toString())).ts(now).build());
 }
 /** WARN sends no command to the device, but it must leave a trace; throttled so it does not flood the audit log. */
 private void auditWarning(DeviceMetricsDTO metrics,long minIntervalMs){
  long now=System.currentTimeMillis();
  Long last=lastWarningAuditMs.get(metrics.deviceId());
  if(last!=null&&now-last<minIntervalMs)return;
  lastWarningAuditMs.put(metrics.deviceId(),now);
  audit.publish(AuditLogEntry.builder().type("DECISION").eventType("PREDICTIVE_WARNING")
   .deviceId(metrics.deviceId()).action(DeviceCommand.WARN.name()).policyVersion(metrics.policyVersion())
   .riskScore(metrics.riskScore()).confidence(metrics.confidence()).timeToThreshold(metrics.timeToThreshold())
   .observabilityScore(metrics.observabilityScore()).metricsHash(sha(metrics.toString())).ts(now).build());
 }
 private String sha(String s){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}

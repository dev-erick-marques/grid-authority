package com.gridauthority.coordinator.application.service;
import com.gridauthority.coordinator.application.dto.*;
import com.gridauthority.coordinator.decision.*;
import com.gridauthority.coordinator.domain.model.*;
import com.gridauthority.coordinator.governance.*;
import com.gridauthority.coordinator.observability.ObservabilityService;
import com.gridauthority.coordinator.prediction.*;
import com.gridauthority.coordinator.infrastructure.registry.DeviceRegistry;
import com.gridauthority.coordinator.infrastructure.repository.*;
import com.gridauthority.coordinator.domain.service.VoltageStatisticsService;
import lombok.RequiredArgsConstructor; import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service; import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException; import java.time.Instant; import java.util.*; import java.util.concurrent.*;
@Service @RequiredArgsConstructor @Slf4j
public class TelemetryIngestionService {
 private final VoltageWindowRepository windows; private final VoltageStatisticsService statsService;
 private final PredictiveAssessor assessor; private final ObservabilityService observability; private final DecisionEngine decision;
 private final ActionSelector selector; private final ActionAuthorization authorization; private final PolicyRepository policies;
 private final MetricsHistoryRepository history; private final DeviceRegistry registry; private final DeviceSurgeStateRepository surge;
 private final StableCycleTracker stableTracker; private final MitigationStateTracker mitigation;
 private final DeviceCommandService dispatcher;
 private final ConcurrentHashMap<String,Object> locks=new ConcurrentHashMap<>(); private final List<SseEmitter> emitters=new CopyOnWriteArrayList<>();
 public void registerEmitter(SseEmitter e){emitters.add(e);e.onCompletion(()->emitters.remove(e));e.onTimeout(()->emitters.remove(e));e.onError(x->emitters.remove(e));}
 public void ingest(DeviceTelemetryDTO t){
   if(t.sourceUrl()!=null&&!t.sourceUrl().isBlank()) registry.register(t.deviceId(),t.sourceUrl());
   windows.recordAndGet(t).ifPresent(w->process(t,w));
 }
 private void process(DeviceTelemetryDTO t, com.gridauthority.coordinator.observability.ObservationWindow observation){
  double[] w = observation.values();
  var lock=locks.computeIfAbsent(t.deviceId(),k->new Object());
  synchronized(lock){
   var s=statsService.compute(w); var p=policies.getActive();
   var f=assessor.assess(w,s.cv(),observation.sampleIntervalSeconds(),
      observability.score(w,observation.received(),observation.expected(),observation.latestAgeMs()),p);
   double slope=f.trend(), accel=f.acceleration(), dev=f.baselineDeviation(), forecastValue=f.forecast();
   double risk=f.riskScore(), conf=f.confidence(), obs=f.observabilityScore(); Double ttt=f.timeToThreshold();
   double lower=p.nominalVoltage()*(1-p.voltageCritical()), upper=p.nominalVoltage()*(1+p.voltageCritical());
   boolean emergency=s.mean()<lower||s.mean()>upper;
   boolean stableForRecovery = decision.isStableForRecovery(risk,s.cv(),slope,conf,dev,ttt,emergency,p);
   long now = System.currentTimeMillis();
   stableTracker.observe(t.deviceId(), stableForRecovery, now);
   long stableDurationMs = stableTracker.stableDurationMs(t.deviceId(), now);

   DeviceCommand desired=decision.decide(risk,conf,ttt,emergency,p);
   desired = decision.applyRecovery(desired, emergency, mitigation.isActive(t.deviceId()), stableDurationMs, p);
   desired=selector.refine(desired);
   DeviceCommand action=authorization.authorize(p,desired);
   var metrics=new DeviceMetricsDTO(t.deviceId(),t.deviceName(),s.mean(),s.std(),s.cv(),t.status(),surge.get(t.deviceId()),
      Instant.now(),slope,accel,dev,forecastValue,risk,conf,ttt,obs,action.name(),p.policyVersion());
   history.add(metrics);
   boolean sent=dispatcher.dispatch(metrics,action);
   if(sent){
     if(action==DeviceCommand.RESTORE_GRID){ mitigation.clear(t.deviceId()); stableTracker.reset(t.deviceId()); }
     else if(action.isMitigation()) mitigation.activate(t.deviceId(),action);
   }
   if(sent && action==DeviceCommand.START_GENERATOR
      && p.allowedActions().contains(DeviceCommand.TRANSFER_PRIORITY_LOAD.name())){
     DeviceMetricsDTO transfer=metricsWithAction(metrics, DeviceCommand.TRANSFER_PRIORITY_LOAD);
     if(dispatcher.dispatch(transfer,DeviceCommand.TRANSFER_PRIORITY_LOAD)) mitigation.activate(t.deviceId(),DeviceCommand.TRANSFER_PRIORITY_LOAD);
   }
   broadcast(metrics);
  }
 }
 private DeviceMetricsDTO metricsWithAction(DeviceMetricsDTO m, DeviceCommand action) {
   return new DeviceMetricsDTO(m.deviceId(),m.deviceName(),m.mean(),m.std(),m.cv(),m.state(),m.surgeState(),m.evaluatedAt(),
      m.trend(),m.acceleration(),m.baselineDeviation(),m.forecast(),m.riskScore(),m.confidence(),m.timeToThreshold(),m.observabilityScore(),action.name(),m.policyVersion());
 }
 private void broadcast(DeviceMetricsDTO m){List<SseEmitter> dead=new ArrayList<>();for(SseEmitter e:emitters)try{e.send(SseEmitter.event().name("metrics").data(m));}catch(IOException x){dead.add(e);}emitters.removeAll(dead);}
}

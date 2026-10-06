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
 private final TrendEstimator trend; private final BaselineEstimator baseline; private final ForecastEngine forecast;
 private final RiskEngine riskEngine; private final ObservabilityService observability; private final DecisionEngine decision;
 private final ActionSelector selector; private final ActionAuthorization authorization; private final PolicyRepository policies;
 private final MetricsHistoryRepository history; private final DeviceRegistry registry; private final DeviceSurgeStateRepository surge;
 private final StableCycleTracker stableTracker;
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
   double slope=trend.slope(w), accel=trend.acceleration(w), b=baseline.estimate(w);
   double dev=baseline.deviation(s.mean(),b);
   double forecastValue=forecast.forecast(s.mean(),slope,p.horizonSeconds());
   double lower=p.nominalVoltage()*(1-p.voltageCritical()), upper=p.nominalVoltage()*(1+p.voltageCritical());
   double target=slope>=0?upper:lower;
   Double ttt=forecast.timeToThreshold(s.mean(),slope,target);
   double cvRisk=Math.min(1,s.cv()/100.0/Math.max(.01,p.cvCritical()));
   double trendRisk=Math.min(1,Math.abs(slope)/Math.max(.001,p.maxRateOfChange()));
   double devRisk=Math.min(1,dev/Math.max(.01,p.voltageCritical()));
   double rateRisk=trendRisk;
   double tttRisk=ttt==null?0:Math.max(0,Math.min(1,1-ttt/p.horizonSeconds()));
   double obs=observability.score(w,observation.received(),observation.expected(),observation.latestAgeMs());
   double conf=riskEngine.confidence(obs,Math.min(1,1/(1+Math.abs(accel))));
   double risk=riskEngine.score(cvRisk,trendRisk,devRisk,rateRisk,tttRisk);
   boolean emergency=s.mean()<lower||s.mean()>upper;
   boolean stableForRecovery = risk <= p.recoveryRisk() && s.cv() / 100.0 <= p.cvWarning()
      && Math.abs(slope) <= p.maxRateOfChange() * 0.25 && conf >= p.minimumConfidence();
   long now = System.currentTimeMillis();
   stableTracker.observe(t.deviceId(), stableForRecovery, now);
   long stableDurationMs = stableTracker.stableDurationMs(t.deviceId(), now);

   DeviceCommand desired=decision.decide(risk,conf,ttt,emergency,p);
   if (stableDurationMs >= p.recoveryStableSeconds() * 1000L) desired = DeviceCommand.RESTORE_GRID;
   desired=selector.refine(desired);
   DeviceCommand action=authorization.authorize(p,desired);
   var metrics=new DeviceMetricsDTO(t.deviceId(),t.deviceName(),s.mean(),s.std(),s.cv(),t.status(),surge.get(t.deviceId()),
      Instant.now(),slope,accel,dev,forecastValue,risk,conf,ttt,obs,action.name(),p.policyVersion());
   history.add(metrics);
   dispatcher.dispatch(metrics,action);
   if(action == DeviceCommand.RESTORE_GRID) stableTracker.reset(t.deviceId());
   if(action==DeviceCommand.START_GENERATOR) {
      DeviceMetricsDTO transfer=metricsWithAction(metrics, DeviceCommand.TRANSFER_PRIORITY_LOAD);
      if(policies.getActive().allowedActions().contains(DeviceCommand.TRANSFER_PRIORITY_LOAD.name())) dispatcher.dispatch(transfer,DeviceCommand.TRANSFER_PRIORITY_LOAD);
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

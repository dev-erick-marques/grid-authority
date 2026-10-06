package com.gridauthority.coordinator.application.service;
import com.gridauthority.coordinator.domain.model.DeviceSurgeState;
import com.gridauthority.coordinator.infrastructure.registry.DeviceRegistry;
import com.gridauthority.coordinator.infrastructure.repository.DeviceSurgeStateRepository;
import com.gridauthority.coordinator.infrastructure.transport.SurgeTransport;
import com.gridauthority.coordinator.signing.LocalSigningService;
import com.gridauthority.coordinator.application.dto.SignedCommandPayload;
import lombok.RequiredArgsConstructor; import org.springframework.stereotype.Service;
@Service @RequiredArgsConstructor
public class CoordinatorSurgeService {
 private final DeviceRegistry registry; private final SurgeTransport transport; private final DeviceSurgeStateRepository repo; private final LocalSigningService signing;
 public DeviceSurgeState getSurgeState(String id){return repo.get(id);}
 public void startSurge(String id){dispatch(id,DeviceSurgeState.SURGE_ACTIVE,"SURGE_START");}
 public void startSag(String id){dispatch(id,DeviceSurgeState.SAG_ACTIVE,"SAG_START");}
 public void stopSurge(String id){dispatch(id,DeviceSurgeState.INACTIVE,"SURGE_STOP");}
 public void startCycle(String id){dispatch(id,DeviceSurgeState.CYCLE_ACTIVE,"SURGE_CYCLE_START");}
 public void stopCycle(String id){dispatch(id,DeviceSurgeState.INACTIVE,"SURGE_CYCLE_STOP");}
 private void dispatch(String id,DeviceSurgeState state,String action){
  registry.resolve(id).ifPresentOrElse(url->{SignedCommandPayload p=signing.signAction(id,action); transport.send(url,id,action,p);repo.set(id,state);},()->{throw new IllegalStateException("Device not registered: "+id);});
 }
}

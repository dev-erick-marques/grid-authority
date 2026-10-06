package com.gridauthority.coordinator.domain.service;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
class StabilityEvaluatorTest {
 @Test void thresholdIsExposedAsObservationSignal(){
  var t=new StabilityEvaluator().getThreshold();
  assertThat(t.thresholdCV()).isEqualTo(10.0);
  assertThat(t.standard()).isEqualTo("CV");
 }
}

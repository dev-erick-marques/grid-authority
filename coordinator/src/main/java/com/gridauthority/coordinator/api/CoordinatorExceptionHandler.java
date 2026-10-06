package com.gridauthority.coordinator.api;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.ResponseEntity;
import java.util.Map;
@RestControllerAdvice
public class CoordinatorExceptionHandler {
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String,String>> handle(Exception e){
    return ResponseEntity.internalServerError().body(Map.of("error",e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()));
  }
}

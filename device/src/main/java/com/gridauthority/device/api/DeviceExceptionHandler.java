package com.gridauthority.device.api;

import com.gridauthority.device.domain.exception.DeviceAlreadyActiveException;
import com.gridauthority.device.domain.exception.DeviceAlreadyShutdownException;
import com.gridauthority.device.domain.exception.SignatureVerificationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Map;

@ControllerAdvice
public class DeviceExceptionHandler {

    @ExceptionHandler(DeviceAlreadyShutdownException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleAlreadyShutdown(DeviceAlreadyShutdownException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(DeviceAlreadyActiveException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleAlreadyActive(DeviceAlreadyActiveException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleUnknownCommand(IllegalArgumentException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(SignatureVerificationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleSignatureVerification(SignatureVerificationException ex) {
        return Map.of("error",  "request_authentication_failed");
    }
}

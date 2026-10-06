package com.gridauthority.device.api;

import com.gridauthority.device.domain.exception.DeviceAlreadyActiveException;
import com.gridauthority.device.domain.exception.DeviceAlreadyShutdownException;
import com.gridauthority.device.domain.exception.SignatureVerificationException;
import com.gridauthority.device.domain.exception.UnknownCommandException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class DeviceExceptionHandler {

    public enum ErrorCode {
        DEVICE_ALREADY_SHUTDOWN,
        DEVICE_ALREADY_ACTIVE,
        UNKNOWN_COMMAND,
        SIGNATURE_VERIFICATION_FAILED,
        VALIDATION_ERROR
    }

    private ProblemDetail buildProblemDetail(HttpStatus status, String message, ErrorCode errorCode) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message);
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", errorCode.name());
        return problem;
    }

    @ExceptionHandler(DeviceAlreadyShutdownException.class)
    public ProblemDetail handleAlreadyShutdown(DeviceAlreadyShutdownException ex) {
        return buildProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), ErrorCode.DEVICE_ALREADY_SHUTDOWN);
    }

    @ExceptionHandler(DeviceAlreadyActiveException.class)
    public ProblemDetail handleAlreadyActive(DeviceAlreadyActiveException ex) {
        return buildProblemDetail(HttpStatus.CONFLICT, ex.getMessage(), ErrorCode.DEVICE_ALREADY_ACTIVE);
    }

    @ExceptionHandler(UnknownCommandException.class)
    public ProblemDetail handleUnknownCommand(UnknownCommandException ex) {
        return buildProblemDetail(HttpStatus.BAD_REQUEST, ex.getMessage(), ErrorCode.UNKNOWN_COMMAND);
    }

    @ExceptionHandler(SignatureVerificationException.class)
    public ProblemDetail handleSignatureVerification(SignatureVerificationException ex) {
        log.warn("[VERIFY] Signature verification failed: {}", ex.getMessage());
        return buildProblemDetail(HttpStatus.BAD_REQUEST,
                "request_authentication_failed", ErrorCode.SIGNATURE_VERIFICATION_FAILED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        List<String> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        ProblemDetail problem = buildProblemDetail(HttpStatus.BAD_REQUEST,
                "Request validation failed", ErrorCode.VALIDATION_ERROR);
        problem.setProperty("violations", violations);
        return problem;
    }
}
package com.gridauthority.coordinator.api;

import com.gridauthority.coordinator.domain.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class CoordinatorExceptionHandler {

    public enum ErrorCode {
        KMS_UNAVAILABLE,
        KMS_PUBLIC_KEY_NOT_LOADED,
        KMS_SIGNING_FAILED,
        CANONICAL_SERIALIZATION_FAILED
    }

    private ProblemDetail buildProblemDetail(HttpStatus status, String message, ErrorCode errorCode) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message);
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", errorCode.name());
        return problem;
    }

    @ExceptionHandler(KmsUnavailableException.class)
    public ProblemDetail handleKmsUnavailable(KmsUnavailableException ex) {
        log.error("[KMS] Service unavailable: {}", ex.getMessage(), ex);
        return buildProblemDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), ErrorCode.KMS_UNAVAILABLE);
    }

    @ExceptionHandler(KmsPublicKeyNotLoadedException.class)
    public ProblemDetail handleKmsPublicKeyNotLoaded(KmsPublicKeyNotLoadedException ex) {
        log.error("[KMS] Public key not loaded: {}", ex.getMessage(), ex);
        return buildProblemDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), ErrorCode.KMS_PUBLIC_KEY_NOT_LOADED);
    }

    @ExceptionHandler(KmsSigningFailedException.class)
    public ProblemDetail handleKmsSigningFailed(KmsSigningFailedException ex) {
        log.error("[KMS] Signing failed: {}", ex.getMessage(), ex);
        return buildProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Command signing failed — check KMS configuration",
                ErrorCode.KMS_SIGNING_FAILED);
    }

    @ExceptionHandler(CanonicalSerializationException.class)
    public ProblemDetail handleCanonicalSerialization(CanonicalSerializationException ex) {
        log.error("[KMS] Canonical serialization error: {}", ex.getMessage(), ex);
        return buildProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Payload serialization error",
                ErrorCode.CANONICAL_SERIALIZATION_FAILED);
    }
}
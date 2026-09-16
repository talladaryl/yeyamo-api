package com.yeyamo_mobile.api.auth_service.exception;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.yeyamo_mobile.api.auth_service.dto.ErrorResponse;
import com.yeyamo_mobile.api.auth_service.dto.FieldErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException exception, WebRequest request) {
        return ResponseEntity.status(exception.getStatus())
                .body(ErrorResponse.of(exception.getCode(), exception.getMessage(), List.of(), correlationId(request)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception, WebRequest request) {
        List<FieldErrorResponse> details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorResponse(error.getField(), error.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("VALIDATION_ERROR", "Requête invalide", details, correlationId(request)));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataConflict(DataIntegrityViolationException exception, WebRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("DATA_CONFLICT", "Ces informations sont déjà utilisées", List.of(), correlationId(request)));
    }

    @ExceptionHandler(RedisConnectionFailureException.class)
    public ResponseEntity<ErrorResponse> handleOtpStoreUnavailable(RedisConnectionFailureException exception, WebRequest request) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of("OTP_SERVICE_UNAVAILABLE",
                        "Le service de vérification est indisponible. Réessayez dans quelques instants.",
                        List.of(), correlationId(request)));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccess(DataAccessException exception, WebRequest request) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of("DATABASE_UNAVAILABLE",
                        "Le service d'inscription est temporairement indisponible. Réessayez dans quelques instants.",
                        List.of(), correlationId(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception, WebRequest request) {
        log.error("Unhandled auth-service exception correlationId={} type={}", correlationId(request),
                exception.getClass().getSimpleName(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "Une erreur interne est survenue", List.of(), correlationId(request)));
    }

    private String correlationId(WebRequest request) {
        String header = request.getHeader("X-Correlation-Id");
        return header == null || header.isBlank() ? null : header;
    }
}

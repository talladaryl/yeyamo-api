package com.yeyamo_mobile.api.interaction_service.interfaces.rest;

import java.time.Instant;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.yeyamo_mobile.api.interaction_service.application.InteractionException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InteractionException.class)
    ResponseEntity<Map<String, Object>> interaction(InteractionException exception) {
        HttpStatus status = exception.getCode().endsWith("NOT_FOUND")
                ? HttpStatus.NOT_FOUND
                : exception.getCode().endsWith("FORBIDDEN") ? HttpStatus.FORBIDDEN : HttpStatus.BAD_REQUEST;
        return body(status, exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    ResponseEntity<Map<String, Object>> domain(RuntimeException exception) {
        return body(HttpStatus.BAD_REQUEST, "INVALID_INTERACTION_OPERATION", exception.getMessage());
    }

    @ExceptionHandler({ MethodArgumentNotValidException.class, ConstraintViolationException.class })
    ResponseEntity<Map<String, Object>> validation(Exception exception) {
        return body(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, Object>> conflict(DataIntegrityViolationException exception) {
        return body(HttpStatus.CONFLICT, "INTERACTION_CONFLICT", "The interaction already exists or conflicts");
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "code", code,
                "message", message,
                "timestamp", Instant.now().toString()));
    }
}

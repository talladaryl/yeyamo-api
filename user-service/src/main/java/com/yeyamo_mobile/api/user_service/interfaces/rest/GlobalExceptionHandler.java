package com.yeyamo_mobile.api.user_service.interfaces.rest;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.yeyamo_mobile.api.user_service.application.exception.UserProfileException;
import com.yeyamo_mobile.api.user_service.interfaces.rest.dto.ErrorResponse;
import com.yeyamo_mobile.api.user_service.interfaces.rest.dto.FieldErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UserProfileException.class)
    ResponseEntity<ErrorResponse> business(UserProfileException exception, HttpServletRequest request) {
        return ResponseEntity.status(exception.getStatus()).body(ErrorResponse.of(exception.getCode(),
                exception.getMessage(), List.of(), request.getHeader("X-Correlation-ID")));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<FieldErrorResponse> details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorResponse(error.getField(), error.getDefaultMessage())).toList();
        return ResponseEntity.badRequest().body(ErrorResponse.of("VALIDATION_ERROR", "Données invalides", details,
                request.getHeader("X-Correlation-ID")));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception exception, HttpServletRequest request) {
        log.error("Unexpected user-service error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.of("INTERNAL_ERROR",
                "Une erreur interne est survenue", List.of(), request.getHeader("X-Correlation-ID")));
    }
}

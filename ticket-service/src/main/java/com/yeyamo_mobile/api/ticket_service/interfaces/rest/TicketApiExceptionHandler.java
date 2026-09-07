package com.yeyamo_mobile.api.ticket_service.interfaces.rest;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = UserTicketController.class)
class TicketApiExceptionHandler {

    @ExceptionHandler(TokenRefreshRequiredException.class)
    ResponseEntity<Problem> tokenRefreshRequired(TokenRefreshRequiredException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new Problem("TOKEN_REFRESH_REQUIRED", exception.getMessage(), Instant.now()));
    }

    record Problem(String code, String detail, Instant timestamp) {
    }
}

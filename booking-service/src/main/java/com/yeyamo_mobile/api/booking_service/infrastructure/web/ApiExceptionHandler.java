package com.yeyamo_mobile.api.booking_service.infrastructure.web;

import java.time.Instant;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import com.yeyamo_mobile.api.booking_service.domain.BookingException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BookingException.class)
    ResponseEntity<Problem> booking(BookingException e) {
        HttpStatus status;
        if (e.code().contains("FORBIDDEN")) {
            status = HttpStatus.FORBIDDEN;
        } else if (e.code().contains("NOT_FOUND")) {
            status = HttpStatus.NOT_FOUND;
        } else if ("INVALID_PLACE".equals(e.code())) {
            status = HttpStatus.UNPROCESSABLE_ENTITY;
        } else {
            status = HttpStatus.CONFLICT;
        }
        return response(status, e.code(), e.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<Problem> missing(Exception e) {
        return response(HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND", e.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    ResponseEntity<Problem> bad(Exception e) {
        return response(HttpStatus.BAD_REQUEST, "BOOKING_INVALID", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Problem> validation(MethodArgumentNotValidException e) {
        String d = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .orElse("Invalid request");
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", d);
    }

    private ResponseEntity<Problem> response(HttpStatus s, String c, String d) {
        return ResponseEntity.status(s).body(new Problem(c, d, Instant.now()));
    }

    record Problem(String code, String detail, Instant timestamp) {}
}

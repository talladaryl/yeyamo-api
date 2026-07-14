package com.yeyamo_mobile.api.discovery_service.infrastructure.web;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
@RestControllerAdvice public class ApiExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class,MethodArgumentNotValidException.class})
    ResponseEntity<Map<String,Object>> badRequest(Exception e) { return ResponseEntity.badRequest().body(Map.of("timestamp",Instant.now(),"status",400,"code","INVALID_REQUEST","message",e.getMessage())); }
}

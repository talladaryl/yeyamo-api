package com.yeyamo_mobile.api.discovery_service.infrastructure.web;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import com.yeyamo_mobile.api.discovery_service.infrastructure.maps.ExternalMapsException;
@RestControllerAdvice public class ApiExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class,MethodArgumentNotValidException.class})
    ResponseEntity<Map<String,Object>> badRequest(Exception e) { return ResponseEntity.badRequest().body(Map.of("timestamp",Instant.now(),"status",400,"code","INVALID_REQUEST","message",e.getMessage())); }
    @ExceptionHandler(ExternalMapsException.class) ResponseEntity<Map<String,Object>> maps(ExternalMapsException e,@RequestHeader(value="X-Correlation-Id",required=false)String correlationId){Map<String,Object>body=new java.util.LinkedHashMap<>();body.put("timestamp",Instant.now());body.put("status",e.status().value());body.put("code",e.code());body.put("message",e.getMessage());if(correlationId!=null)body.put("correlationId",correlationId);return ResponseEntity.status(e.status()).body(body);}
}

package com.yeyamo_mobile.api.notification_service.infrastructure.web;
import java.time.Instant;import java.util.*;import org.springframework.http.*;import org.springframework.web.bind.MethodArgumentNotValidException;import org.springframework.web.bind.annotation.*;
@RestControllerAdvice public class ApiExceptionHandler {
 @ExceptionHandler({IllegalArgumentException.class,MethodArgumentNotValidException.class})
 ResponseEntity<Map<String,Object>> bad(Exception e,@RequestHeader(value="X-Correlation-Id",required=false)String correlation){return response(400,"VALIDATION_ERROR",e.getMessage(),correlation);}
 @ExceptionHandler(NoSuchElementException.class)
 ResponseEntity<Map<String,Object>> missing(Exception e,@RequestHeader(value="X-Correlation-Id",required=false)String correlation){return response(404,"RESOURCE_NOT_FOUND",e.getMessage(),correlation);}
 @ExceptionHandler(IllegalStateException.class)
 ResponseEntity<Map<String,Object>> conflict(Exception e,@RequestHeader(value="X-Correlation-Id",required=false)String correlation){return response(409,"INVALID_STATE",e.getMessage(),correlation);}
 private ResponseEntity<Map<String,Object>> response(int status,String code,String message,String correlation){Map<String,Object>body=new LinkedHashMap<>();body.put("timestamp",Instant.now());body.put("status",status);body.put("code",code);body.put("message",Objects.toString(message,code));body.put("correlationId",correlation);body.put("errors",List.of());return ResponseEntity.status(status).body(body);}
}

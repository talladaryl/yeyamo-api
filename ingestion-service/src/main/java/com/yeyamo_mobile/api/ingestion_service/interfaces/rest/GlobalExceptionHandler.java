package com.yeyamo_mobile.api.ingestion_service.interfaces.rest;
import java.time.Instant;import java.util.Map;import org.springframework.http.*;import org.springframework.web.bind.MethodArgumentNotValidException;import org.springframework.web.bind.annotation.*;
import com.yeyamo_mobile.api.ingestion_service.application.IngestionException;
import jakarta.validation.ConstraintViolationException;
@RestControllerAdvice
public class GlobalExceptionHandler{
 @ExceptionHandler(IngestionException.class)ResponseEntity<Map<String,Object>> ingestion(IngestionException e){return body(HttpStatus.NOT_FOUND,e.getCode(),e.getMessage());}
 @ExceptionHandler({IllegalArgumentException.class,IllegalStateException.class})ResponseEntity<Map<String,Object>> bad(RuntimeException e){return body(HttpStatus.BAD_REQUEST,"INVALID_IMPORT",e.getMessage());}
 @ExceptionHandler(MethodArgumentNotValidException.class)ResponseEntity<Map<String,Object>> validation(MethodArgumentNotValidException e){return body(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","Request validation failed");}
 @ExceptionHandler(ConstraintViolationException.class)ResponseEntity<Map<String,Object>> constraint(ConstraintViolationException e){return body(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR",e.getMessage());}
 private ResponseEntity<Map<String,Object>> body(HttpStatus s,String c,String m){return ResponseEntity.status(s).body(Map.of("code",c,"message",m,"timestamp",Instant.now().toString()));}
}

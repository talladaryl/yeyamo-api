package com.yeyamo_mobile.api.content_service.interfaces.rest;
import java.time.Instant;import java.util.Map;import org.springframework.http.*;import org.springframework.web.bind.MethodArgumentNotValidException;import org.springframework.web.bind.annotation.*;
import com.yeyamo_mobile.api.content_service.application.ContentException;import jakarta.validation.ConstraintViolationException;
@RestControllerAdvice
public class GlobalExceptionHandler{
 @ExceptionHandler(ContentException.class)ResponseEntity<Map<String,Object>> content(ContentException e){HttpStatus s=e.getCode().endsWith("NOT_FOUND")?HttpStatus.NOT_FOUND:e.getCode().endsWith("FORBIDDEN")?HttpStatus.FORBIDDEN:HttpStatus.BAD_REQUEST;return body(s,e.getCode(),e.getMessage());}
 @ExceptionHandler({IllegalArgumentException.class,IllegalStateException.class})ResponseEntity<Map<String,Object>> domain(RuntimeException e){return body(HttpStatus.BAD_REQUEST,"INVALID_POST_OPERATION",e.getMessage());}
 @ExceptionHandler({MethodArgumentNotValidException.class,ConstraintViolationException.class})ResponseEntity<Map<String,Object>> validation(Exception e){return body(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","Request validation failed");}
 private ResponseEntity<Map<String,Object>> body(HttpStatus s,String c,String m){return ResponseEntity.status(s).body(Map.of("code",c,"message",m,"timestamp",Instant.now().toString()));}
}

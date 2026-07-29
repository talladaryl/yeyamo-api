package com.yeyamo_mobile.api.support_service.infrastructure.web;
import java.time.Instant;import java.util.*;import org.springframework.http.*;import org.springframework.web.bind.MethodArgumentNotValidException;import org.springframework.web.bind.annotation.*;
@RestControllerAdvice public class ApiExceptionHandler {
 @ExceptionHandler({IllegalArgumentException.class,MethodArgumentNotValidException.class})ResponseEntity<Map<String,Object>> bad(Exception e){return response(400,"INVALID_REQUEST",e.getMessage());}
 @ExceptionHandler(NoSuchElementException.class)ResponseEntity<Map<String,Object>> missing(Exception e){return response(404,"SUPPORT_CONVERSATION_NOT_FOUND",e.getMessage());}
 private ResponseEntity<Map<String,Object>> response(int status,String code,String message){return ResponseEntity.status(status).body(Map.of("timestamp",Instant.now(),"status",status,"code",code,"message",Objects.toString(message,code)));}
}

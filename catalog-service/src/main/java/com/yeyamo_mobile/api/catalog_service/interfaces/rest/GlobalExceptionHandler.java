package com.yeyamo_mobile.api.catalog_service.interfaces.rest;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import com.yeyamo_mobile.api.catalog_service.application.CatalogException;
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(CatalogException.class) ResponseEntity<Map<String,Object>> catalog(CatalogException ex){
        HttpStatus status=ex.getCode().endsWith("NOT_FOUND")?HttpStatus.NOT_FOUND:
                ex.getCode().endsWith("EXISTS")?HttpStatus.CONFLICT:HttpStatus.BAD_REQUEST;
        return body(status,ex.getCode(),ex.getMessage());
    }
    @ExceptionHandler({IllegalArgumentException.class,IllegalStateException.class})
    ResponseEntity<Map<String,Object>> domain(RuntimeException ex){return body(HttpStatus.BAD_REQUEST,"INVALID_CATALOG_OPERATION",ex.getMessage());}
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String,Object>> validation(MethodArgumentNotValidException ex){return body(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","Request validation failed");}
    private ResponseEntity<Map<String,Object>> body(HttpStatus s,String c,String m){
        return ResponseEntity.status(s).body(Map.of("code",c,"message",m,"timestamp",Instant.now().toString()));}
}

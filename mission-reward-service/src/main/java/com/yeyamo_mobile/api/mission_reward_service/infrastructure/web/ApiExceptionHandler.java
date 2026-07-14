package com.yeyamo_mobile.api.mission_reward_service.infrastructure.web;
import java.time.Instant;import java.util.*;import org.springframework.http.*;import org.springframework.web.bind.MethodArgumentNotValidException;import org.springframework.web.bind.annotation.*;
@RestControllerAdvice public class ApiExceptionHandler{
 @ExceptionHandler(NoSuchElementException.class)ResponseEntity<Problem>notFound(Exception e){return response(HttpStatus.NOT_FOUND,"MISSION_NOT_FOUND",e.getMessage());}
 @ExceptionHandler({IllegalArgumentException.class,IllegalStateException.class})ResponseEntity<Problem>bad(Exception e){return response(HttpStatus.BAD_REQUEST,"MISSION_INVALID",e.getMessage());}
 @ExceptionHandler(MethodArgumentNotValidException.class)ResponseEntity<Problem>validation(MethodArgumentNotValidException e){String detail=e.getBindingResult().getFieldErrors().stream().findFirst().map(f->f.getField()+" "+f.getDefaultMessage()).orElse("Invalid request");return response(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR",detail);}
 private ResponseEntity<Problem>response(HttpStatus status,String code,String detail){return ResponseEntity.status(status).body(new Problem(code,detail,Instant.now()));}
 record Problem(String code,String detail,Instant timestamp){}
}

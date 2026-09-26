package com.yeyamo_mobile.api.media_service.interfaces.rest;
import java.time.Instant;import java.util.Map;import org.slf4j.Logger;import org.slf4j.LoggerFactory;import org.springframework.http.*;import org.springframework.web.HttpMediaTypeNotSupportedException;import org.springframework.web.bind.annotation.*;import org.springframework.web.multipart.MaxUploadSizeExceededException;
import com.yeyamo_mobile.api.media_service.application.MediaException;
@RestControllerAdvice
public class GlobalExceptionHandler{
 private static final Logger log=LoggerFactory.getLogger(GlobalExceptionHandler.class);
 @ExceptionHandler(MediaException.class)ResponseEntity<Map<String,Object>> media(MediaException e){
  HttpStatus s=switch(e.getCode()){
   case "MEDIA_NOT_FOUND"        -> HttpStatus.NOT_FOUND;
   case "MEDIA_FORBIDDEN"        -> HttpStatus.FORBIDDEN;
   case "MEDIA_DUPLICATE"        -> HttpStatus.CONFLICT;
   case "QUOTA_EXCEEDED"         -> HttpStatus.TOO_MANY_REQUESTS;
   case "STORAGE_UNAVAILABLE"    -> HttpStatus.SERVICE_UNAVAILABLE;
   case "SIGNED_URL_EXPIRED",
        "SIGNED_URL_INVALID",
        "SIGNED_URL_REQUIRED"    -> HttpStatus.UNAUTHORIZED;
   case "USAGE_TYPE_MISMATCH",
        "UNSUPPORTED_DOCUMENT_TYPE" -> HttpStatus.UNPROCESSABLE_ENTITY;
   default                       -> HttpStatus.BAD_REQUEST;
  };
  return body(s,e.getCode(),e.getMessage());}
 @ExceptionHandler(MaxUploadSizeExceededException.class)ResponseEntity<Map<String,Object>> size(MaxUploadSizeExceededException e){return body(HttpStatus.PAYLOAD_TOO_LARGE,"MEDIA_TOO_LARGE","Uploaded media is too large");}
 @ExceptionHandler(HttpMediaTypeNotSupportedException.class)ResponseEntity<Map<String,Object>> unsupportedMediaType(HttpMediaTypeNotSupportedException e){
  // Deliberately log only the rejected protocol header: never the bytes, file
  // name, body or bearer token of an upload request.
  log.warn("Rejected media upload with unsupported Content-Type={}", e.getContentType());
  return body(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"UNSUPPORTED_MEDIA_TYPE","This endpoint accepts multipart/form-data uploads");}
 private ResponseEntity<Map<String,Object>> body(HttpStatus s,String c,String m){return ResponseEntity.status(s).body(Map.of("code",c,"message",m,"timestamp",Instant.now().toString()));}
}

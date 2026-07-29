package com.yeyamo_mobile.api.support_service.infrastructure.web;
import static com.yeyamo_mobile.api.support_service.application.SupportDtos.*;
import java.time.Instant;import java.util.UUID;import com.yeyamo_mobile.api.support_service.application.SupportAdminService;import com.yeyamo_mobile.api.support_service.domain.*;
import jakarta.validation.Valid;import org.springframework.data.domain.*;import org.springframework.data.web.PageableDefault;import org.springframework.format.annotation.DateTimeFormat;import org.springframework.http.HttpStatus;import org.springframework.security.oauth2.jwt.Jwt;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/admin/support/conversations")
public class SupportAdminController {
 private final SupportAdminService service; public SupportAdminController(SupportAdminService s){service=s;}
 @GetMapping public Page<ConversationSummary> list(@RequestParam(required=false)SupportStatus status,@RequestParam(required=false)SupportPriority priority,@RequestParam(required=false)String assigneeId,@RequestParam(required=false)String userId,@RequestParam(required=false)String search,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)Instant createdFrom,@RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME)Instant createdTo,@PageableDefault(size=25,sort="updatedAt",direction=Sort.Direction.DESC)Pageable pageable){return service.list(status,priority,assigneeId,userId,search,createdFrom,createdTo,pageable);}
 @GetMapping("/{id}") public ConversationDetail detail(@PathVariable UUID id){return service.detail(id);}
 @PostMapping("/{id}/messages") @ResponseStatus(HttpStatus.CREATED) public MessageResponse message(@PathVariable UUID id,@Valid @RequestBody MessageRequest r,Jwt jwt,@RequestHeader(value="X-Correlation-Id",required=false)String c){return service.message(id,jwt.getSubject(),r,c);}
 @PostMapping("/{id}/notes") @ResponseStatus(HttpStatus.CREATED) public NoteResponse note(@PathVariable UUID id,@Valid @RequestBody NoteRequest r,Jwt jwt,@RequestHeader(value="X-Correlation-Id",required=false)String c){return service.note(id,jwt.getSubject(),r,c);}
 @PatchMapping("/{id}/assign") public ConversationSummary assign(@PathVariable UUID id,@Valid @RequestBody AssignRequest r,Jwt jwt,@RequestHeader(value="X-Correlation-Id",required=false)String c){return service.assign(id,jwt.getSubject(),r,c);}
 @PatchMapping("/{id}/status") public ConversationSummary status(@PathVariable UUID id,@Valid @RequestBody StatusRequest r,Jwt jwt,@RequestHeader(value="X-Correlation-Id",required=false)String c){return service.status(id,jwt.getSubject(),r,c);}
 @PatchMapping("/{id}/priority") public ConversationSummary priority(@PathVariable UUID id,@Valid @RequestBody PriorityRequest r,Jwt jwt,@RequestHeader(value="X-Correlation-Id",required=false)String c){return service.priority(id,jwt.getSubject(),r,c);}
}

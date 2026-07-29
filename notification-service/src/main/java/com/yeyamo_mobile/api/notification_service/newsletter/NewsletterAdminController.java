package com.yeyamo_mobile.api.notification_service.newsletter;
import static com.yeyamo_mobile.api.notification_service.newsletter.NewsletterDtos.*;
import java.util.UUID;import jakarta.validation.Valid;import org.springframework.data.domain.*;import org.springframework.data.web.PageableDefault;import org.springframework.http.HttpStatus;import org.springframework.security.oauth2.jwt.Jwt;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/admin/newsletters") public class NewsletterAdminController {
 private final NewsletterApplicationService service;public NewsletterAdminController(NewsletterApplicationService s){service=s;}
 @GetMapping public Page<CampaignResponse> list(@RequestParam(required=false)NewsletterStatus status,@RequestParam(required=false)String search,@PageableDefault(size=25,sort="createdAt",direction=Sort.Direction.DESC)Pageable p){return service.list(status,search,p);}
 @GetMapping("/{id}")public CampaignResponse get(@PathVariable UUID id){return service.get(id);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED)public CampaignResponse create(@Valid@RequestBody CampaignRequest r,Jwt jwt){return service.create(jwt.getSubject(),r);}
 @PutMapping("/{id}")public CampaignResponse update(@PathVariable UUID id,@Valid@RequestBody CampaignRequest r){return service.update(id,r);}
 @PostMapping("/{id}/schedule")public CampaignResponse schedule(@PathVariable UUID id,@Valid@RequestBody ScheduleRequest r){return service.schedule(id,r);}
 @PostMapping("/{id}/send")public CampaignResponse send(@PathVariable UUID id){return service.send(id);}
 @PostMapping("/{id}/pause")public CampaignResponse pause(@PathVariable UUID id){return service.pause(id);}
 @PostMapping("/{id}/cancel")public CampaignResponse cancel(@PathVariable UUID id){return service.cancel(id);}
 @GetMapping("/{id}/stats")public StatsResponse stats(@PathVariable UUID id){return service.stats(id);}
}

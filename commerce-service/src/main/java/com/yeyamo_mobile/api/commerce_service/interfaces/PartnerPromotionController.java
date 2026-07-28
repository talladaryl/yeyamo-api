package com.yeyamo_mobile.api.commerce_service.interfaces;

import static com.yeyamo_mobile.api.commerce_service.domain.CommerceTypes.*;
import com.yeyamo_mobile.api.commerce_service.persistence.Promotion;
import com.yeyamo_mobile.api.commerce_service.persistence.PromotionRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/commerce/partners/{partnerId}/promotions")
public class PartnerPromotionController {
    private final PromotionRepository promotions;
    private final RestClient partners;
    public PartnerPromotionController(PromotionRepository promotions,@Value("${yeyamo.services.partner-url:http://partner-service:8080}")String partnerUrl){this.promotions=promotions;this.partners=RestClient.create(partnerUrl);}
    public record Request(@NotBlank String code,@NotBlank String name,String description,@NotNull DiscountType discountType,@NotNull@DecimalMin("0")BigDecimal discountValue,BigDecimal maximumDiscount,@NotNull@DecimalMin("0")BigDecimal minimumOrderAmount,Long usageLimit,Long usageLimitPerUser,@NotNull Instant startsAt,@NotNull Instant endsAt,Set<ProductType>applicableProductTypes,Set<String>applicableEntityIds){}
    @GetMapping public Page<Promotion>list(@PathVariable String partnerId,@RequestParam(required=false)PromotionStatus status,Pageable pageable,@RequestHeader(HttpHeaders.AUTHORIZATION)String auth){allow(partnerId,auth);return status==null?promotions.findByPartnerId(partnerId,pageable):promotions.findByPartnerIdAndStatus(partnerId,status,pageable);}
    @GetMapping("/{id}")public Promotion one(@PathVariable String partnerId,@PathVariable UUID id,@RequestHeader(HttpHeaders.AUTHORIZATION)String auth){allow(partnerId,auth);return owned(partnerId,id);}
    @PostMapping public Promotion create(@PathVariable String partnerId,@Valid@RequestBody Request r,@RequestHeader(HttpHeaders.AUTHORIZATION)String auth){allow(partnerId,auth);validate(r,null);Promotion p=new Promotion();p.id=UUID.randomUUID();p.partnerId=partnerId;p.status=PromotionStatus.ACTIVE;copy(p,r);return promotions.save(p);}
    @PutMapping("/{id}")public Promotion update(@PathVariable String partnerId,@PathVariable UUID id,@Valid@RequestBody Request r,@RequestHeader(HttpHeaders.AUTHORIZATION)String auth){allow(partnerId,auth);validate(r,id);Promotion p=owned(partnerId,id);copy(p,r);return promotions.save(p);}
    @PostMapping("/{id}/disable")public Promotion disable(@PathVariable String partnerId,@PathVariable UUID id,@RequestHeader(HttpHeaders.AUTHORIZATION)String auth){allow(partnerId,auth);Promotion p=owned(partnerId,id);p.status=PromotionStatus.INACTIVE;return promotions.save(p);}
    private void validate(Request r,UUID current){if(!r.startsAt().isBefore(r.endsAt()))throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Invalid promotion period");if(r.usageLimit()!=null&&r.usageLimit()<1||r.usageLimitPerUser()!=null&&r.usageLimitPerUser()<1)throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Invalid usage limit");boolean duplicate=promotions.findAll().stream().anyMatch(p->!p.id.equals(current)&&p.code.equalsIgnoreCase(r.code()));if(duplicate)throw new ResponseStatusException(HttpStatus.CONFLICT,"Promotion code already used");}
    private void copy(Promotion p,Request r){p.code=r.code().trim().toUpperCase(Locale.ROOT);p.name=r.name();p.description=r.description();p.discountType=r.discountType();p.discountValue=r.discountValue();p.maximumDiscount=r.maximumDiscount();p.minimumOrderAmount=r.minimumOrderAmount();p.usageLimit=r.usageLimit();p.usageLimitPerUser=r.usageLimitPerUser();p.startsAt=r.startsAt();p.endsAt=r.endsAt();p.applicableProductTypes=r.applicableProductTypes()==null?"":String.join(",",r.applicableProductTypes().stream().map(Enum::name).toList());p.applicableEntityIds=r.applicableEntityIds()==null?"":String.join(",",r.applicableEntityIds());}
    private Promotion owned(String partnerId,UUID id){return promotions.findById(id).filter(p->partnerId.equals(p.partnerId)).orElseThrow();}
    private void allow(String partnerId,String auth){Map<?,?>r=partners.get().uri("/api/v1/partners/{p}/staff/permissions/{x}",partnerId,"partner:finance-view").header(HttpHeaders.AUTHORIZATION,auth).retrieve().body(Map.class);if(r==null||!Boolean.TRUE.equals(r.get("allowed")))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Partner promotion permission denied");}
}

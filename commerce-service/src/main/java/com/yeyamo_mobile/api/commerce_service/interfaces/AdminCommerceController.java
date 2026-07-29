package com.yeyamo_mobile.api.commerce_service.interfaces;
import java.time.Instant;import java.util.UUID;import org.springframework.data.domain.*;import org.springframework.security.core.Authentication;import org.springframework.web.bind.annotation.*;
import com.yeyamo_mobile.api.commerce_service.application.AdminCommerceService;import com.yeyamo_mobile.api.commerce_service.application.AdminCommerceService.*;import com.yeyamo_mobile.api.commerce_service.persistence.*;import static com.yeyamo_mobile.api.commerce_service.domain.CommerceTypes.*;
@RestController@RequestMapping("/api/v1/commerce/admin")public class AdminCommerceController{
 private final AdminCommerceService service;public AdminCommerceController(AdminCommerceService service){this.service=service;}
 @GetMapping("/promotions")public Page<Promotion>promotions(@RequestParam(required=false)String search,@RequestParam(required=false)String partnerId,@RequestParam(required=false)PromotionStatus status,@RequestParam(required=false)DiscountType type,@RequestParam(required=false)Instant from,@RequestParam(required=false)Instant to,Pageable page){return service.promotions(search,partnerId,status,type,from,to,page);}
 @GetMapping("/promotions/{id}")public Promotion promotion(@PathVariable UUID id){return service.promotion(id);}
 @PostMapping("/promotions")public Promotion create(@RequestBody PromotionCommand body,@RequestHeader("Idempotency-Key")String key,Authentication auth){return service.createPromotion(body,key,auth.getName());}
 @PutMapping("/promotions/{id}")public Promotion update(@PathVariable UUID id,@RequestBody PromotionCommand body,@RequestHeader("Idempotency-Key")String key,Authentication auth){return service.updatePromotion(id,body,key,auth.getName());}
 @PostMapping("/promotions/{id}/disable")public Promotion disable(@PathVariable UUID id,@RequestHeader("Idempotency-Key")String key,Authentication auth){return service.disablePromotion(id,key,auth.getName());}
 @GetMapping("/commissions")public Page<CommissionRule>commissions(@RequestParam(required=false)String partnerId,@RequestParam(required=false)ProductType productType,@RequestParam(required=false)String status,@RequestParam(required=false)Instant from,@RequestParam(required=false)Instant to,Pageable page){return service.commissions(partnerId,productType,status,from,to,page);}
 @GetMapping("/commissions/{id}")public CommissionRule commission(@PathVariable UUID id){return service.commission(id);}
 @PostMapping("/commissions")public CommissionRule commission(@RequestBody CommissionCommand body,@RequestHeader("Idempotency-Key")String key,Authentication auth){return service.createCommission(body,key,auth.getName());}
}

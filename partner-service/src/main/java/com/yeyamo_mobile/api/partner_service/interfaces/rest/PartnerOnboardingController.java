package com.yeyamo_mobile.api.partner_service.interfaces.rest;
import org.springframework.web.bind.annotation.*;import com.yeyamo_mobile.api.partner_service.application.CountryKycRequirementsService;
@RestController @RequestMapping("/api/v1/partners/onboarding") public class PartnerOnboardingController {
 private final CountryKycRequirementsService requirements; public PartnerOnboardingController(CountryKycRequirementsService r){requirements=r;}
 @GetMapping("/requirements") public CountryKycRequirementsService.Requirements requirements(@RequestParam String countryCode,@RequestParam String partnerType){return requirements.requirements(countryCode,partnerType,java.util.Set.of());}
}

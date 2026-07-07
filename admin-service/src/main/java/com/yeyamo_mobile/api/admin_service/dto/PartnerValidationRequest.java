package com.yeyamo_mobile.api.admin_service.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PartnerValidationRequest(
        @NotNull UUID partnerId,
        UUID requesterId,
        List<String> kycDocumentUrls,
        List<String> kycDocumentTypes,
        String reviewComment,
        @Min(0) @Max(100) Integer riskScore
) {
    public List<String> safeKycDocumentUrls() {
        return kycDocumentUrls == null ? new ArrayList<>() : kycDocumentUrls;
    }

    public List<String> safeKycDocumentTypes() {
        return kycDocumentTypes == null ? new ArrayList<>() : kycDocumentTypes;
    }
}

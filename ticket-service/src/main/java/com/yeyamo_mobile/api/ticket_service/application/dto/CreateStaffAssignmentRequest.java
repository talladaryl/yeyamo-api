package com.yeyamo_mobile.api.ticket_service.application.dto;

import com.yeyamo_mobile.api.ticket_service.domain.model.StaffRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Request to assign staff to an event
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateStaffAssignmentRequest {
    
    @NotBlank(message = "Event ID is required")
    private String eventId;
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotNull(message = "Role is required")
    private StaffRole role;
    
    @NotNull(message = "Valid from is required")
    private Instant validFrom;
    
    @NotNull(message = "Valid until is required")
    private Instant validUntil;
}

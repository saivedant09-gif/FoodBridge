package com.foodbridge.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodRequest {
    private Long requestId;
    private Long listingId;
    private Long recipientOrganizationId;
    private BigDecimal requestedQuantity;
    private String requestStatus;
    private String message;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Transient/join fields
    private String foodName;
    private String recipientName;
    private String unit;
}

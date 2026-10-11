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
public class Allocation {
    private Long allocationId;
    private Long requestId;
    private Long listingId;
    private Long recipientOrganizationId;
    private BigDecimal allocatedQuantity;
    private String allocationStatus;
    private LocalDateTime confirmedAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Transient/join fields
    private String foodName;
    private String recipientName;
    private String providerName;
    private String unit;
}

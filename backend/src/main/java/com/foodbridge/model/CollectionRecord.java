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
public class CollectionRecord {
    private Long collectionId;
    private Long allocationId;
    private LocalDateTime pickupDeadline;
    private String collectionStatus;
    private Boolean providerConfirmed;
    private LocalDateTime providerConfirmedAt;
    private Boolean recipientConfirmed;
    private LocalDateTime recipientConfirmedAt;
    private LocalDateTime collectedAt;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Transient/join fields
    private String foodName;
    private String providerName;
    private String recipientName;
    private BigDecimal allocatedQuantity;
    private String unit;
}

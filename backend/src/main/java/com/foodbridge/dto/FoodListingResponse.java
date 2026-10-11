package com.foodbridge.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodListingResponse {
    private Long listingId;
    private Long providerOrganizationId;
    private String foodName;
    private String description;
    private String category;
    private BigDecimal totalQuantity;
    private BigDecimal availableQuantity;
    private String unit;
    private String dietaryInfo;
    private String allergenNotes;
    private LocalDateTime preparedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime pickupDeadline;
    private String pickupAddress;
    private String pickupCity;
    private String listingStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Additional fields
    private String providerName;
    private String providerPhone;
}

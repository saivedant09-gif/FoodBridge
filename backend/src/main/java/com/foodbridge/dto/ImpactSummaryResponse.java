package com.foodbridge.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImpactSummaryResponse {
    private Long verifiedOrgs;
    private Long totalListings;
    private Long activeListings;
    private Long completedListings;
    private Long expiredListings;
    private Long totalRequests;
    private Long fulfilledRequests;
    private BigDecimal totalAllocated;
    private Long completedCollections;
    private Long providerUsers;
    private Long recipientUsers;
    private Double fulfillmentRate;
}

package com.foodbridge.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestResponse {
    private Long requestId;
    private Long listingId;
    private String foodName;
    private String unit;
    private BigDecimal requestedQuantity;
    private String requestStatus;
    private String message;
    private Long recipientOrganizationId;
    private String recipientName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

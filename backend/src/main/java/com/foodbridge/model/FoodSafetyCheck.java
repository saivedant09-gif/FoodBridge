package com.foodbridge.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodSafetyCheck {
    private Long checkId;
    private Long listingId;
    private Long checkedBy;
    private String checkType;
    private String checkResult;
    private String notes;
    private LocalDateTime checkedAt;
}

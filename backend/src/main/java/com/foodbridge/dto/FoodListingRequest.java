package com.foodbridge.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodListingRequest {
    @NotBlank
    @Size(min = 2, max = 200)
    private String foodName;

    private String description;

    @NotBlank
    private String category;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal totalQuantity;

    @NotBlank
    @Size(max = 50)
    private String unit;

    private String dietaryInfo;
    private String allergenNotes;
    private String preparedAt; // ISO datetime string

    @NotBlank
    private String expiresAt;

    @NotBlank
    private String pickupDeadline;

    @NotBlank
    private String pickupAddress;

    private String pickupCity;
}

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
public class Organization {
    private Long organizationId;
    private String organizationName;
    private String organizationType;
    private String description;
    private String contactEmail;
    private String contactPhone;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private Double latitude;
    private Double longitude;
    private String verificationStatus;
    private LocalDateTime verifiedAt;
    private Long verifiedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

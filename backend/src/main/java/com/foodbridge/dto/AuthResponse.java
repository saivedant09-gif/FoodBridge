package com.foodbridge.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private Long userId;
    private String email;
    private String fullName;
    private String role;
    private Long organizationId;
    private String organizationName;
    private String organizationStatus;
    private String message;
}

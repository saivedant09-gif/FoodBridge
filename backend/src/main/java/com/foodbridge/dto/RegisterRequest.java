package com.foodbridge.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @NotBlank
    @Size(min = 2, max = 200)
    private String fullName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 64)
    private String password;

    private String phone;

    @NotBlank
    private String role; // PROVIDER or RECIPIENT

    // Organization fields
    @NotBlank
    @Size(min = 2, max = 200)
    private String organizationName;

    @NotBlank
    private String organizationType;

    private String description;

    @NotBlank
    private String address;

    private String city;
    private String state;
    private String pincode;
    private String contactPhone;
}

package com.foodbridge.service;

import com.foodbridge.dto.AuthResponse;
import com.foodbridge.dto.RegisterRequest;
import com.foodbridge.exception.ResourceNotFoundException;
import com.foodbridge.model.Organization;
import com.foodbridge.model.User;
import com.foodbridge.repository.OrganizationRepository;
import com.foodbridge.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public AuthService(UserRepository userRepository,
                       OrganizationRepository organizationRepository,
                       PasswordEncoder passwordEncoder,
                       AuditService auditService,
                       NotificationService notificationService) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Registration request cannot be null");
        }
        if (req.getRole() == null || (!req.getRole().equalsIgnoreCase("PROVIDER") && !req.getRole().equalsIgnoreCase("RECIPIENT"))) {
            throw new IllegalArgumentException("Role must be either PROVIDER or RECIPIENT");
        }
        if (req.getEmail() == null || req.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email cannot be blank");
        }
        if (req.getPassword() == null || req.getPassword().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        if (req.getOrganizationName() == null || req.getOrganizationName().isBlank()) {
            throw new IllegalArgumentException("Organization name cannot be blank");
        }

        String normalizedEmail = req.getEmail().trim().toLowerCase();
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("Email already registered: " + normalizedEmail);
        }

        Organization org = Organization.builder()
                .organizationName(req.getOrganizationName().trim())
                .organizationType(req.getOrganizationType() != null ? req.getOrganizationType().trim() : "GENERAL")
                .description(req.getDescription())
                .contactEmail(normalizedEmail)
                .contactPhone(req.getContactPhone() != null && !req.getContactPhone().isBlank() ? req.getContactPhone().trim() : req.getPhone())
                .address(req.getAddress() != null ? req.getAddress().trim() : "")
                .city(req.getCity() != null ? req.getCity().trim() : "")
                .state(req.getState() != null ? req.getState().trim() : "")
                .pincode(req.getPincode() != null ? req.getPincode().trim() : "")
                .verificationStatus("PENDING")
                .build();
        Long orgId = organizationRepository.save(org);

        User user = User.builder()
                .organizationId(orgId)
                .fullName(req.getFullName().trim())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .phone(req.getPhone())
                .role(req.getRole().toUpperCase().trim())
                .accountStatus("ACTIVE")
                .build();
        Long userId = userRepository.save(user);

        auditService.log(userId, "USER_REGISTERED", "USER", String.valueOf(userId),
                "User registered with organization: " + org.getOrganizationName());

        notificationService.notifyUser(userId, "Welcome to FoodBridge",
                "Your account and organization (" + org.getOrganizationName() + ") have been created. Verification is currently pending.",
                "INFO", "ORGANIZATION", orgId);

        return AuthResponse.builder()
                .userId(userId)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .organizationId(orgId)
                .organizationName(org.getOrganizationName())
                .organizationStatus("PENDING")
                .message("Registration successful. Organization verification is pending approval.")
                .build();
    }

    public AuthResponse getCurrentUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Organization org = null;
        if (user.getOrganizationId() != null) {
            org = organizationRepository.findById(user.getOrganizationId()).orElse(null);
        }

        return AuthResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .organizationId(org != null ? org.getOrganizationId() : null)
                .organizationName(org != null ? org.getOrganizationName() : null)
                .organizationStatus(org != null ? org.getVerificationStatus() : null)
                .message("User details retrieved successfully")
                .build();
    }
}

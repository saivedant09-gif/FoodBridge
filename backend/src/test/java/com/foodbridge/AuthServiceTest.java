package com.foodbridge;

import com.foodbridge.dto.AuthResponse;
import com.foodbridge.dto.RegisterRequest;
import com.foodbridge.exception.ResourceNotFoundException;
import com.foodbridge.model.Organization;
import com.foodbridge.model.User;
import com.foodbridge.repository.OrganizationRepository;
import com.foodbridge.repository.UserRepository;
import com.foodbridge.service.AuditService;
import com.foodbridge.service.AuthService;
import com.foodbridge.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService verifying:
 * - User and organization registration
 * - Duplicate email prevention
 * - Role enforcement (disallowing public ADMIN registration)
 * - Password hashing integration
 * - User detail lookup
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                organizationRepository,
                passwordEncoder,
                auditService,
                notificationService
        );
    }

    @Test
    @DisplayName("Should successfully register provider user and organization")
    void register_success() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Chef Raman");
        req.setEmail("raman@cafeteria.org");
        req.setPassword("SecurePass@123");
        req.setPhone("9876543210");
        req.setRole("PROVIDER");
        req.setOrganizationName("Campus Cafeteria");
        req.setOrganizationType("CAFETERIA");
        req.setAddress("Gate 1, Campus Road");
        req.setCity("Vellore");

        when(userRepository.findByEmail("raman@cafeteria.org")).thenReturn(Optional.empty());
        when(organizationRepository.save(any(Organization.class))).thenReturn(101L);
        when(passwordEncoder.encode("SecurePass@123")).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(201L);

        AuthResponse response = authService.register(req);

        assertNotNull(response);
        assertEquals(201L, response.getUserId());
        assertEquals("raman@cafeteria.org", response.getEmail());
        assertEquals("PROVIDER", response.getRole());
        assertEquals(101L, response.getOrganizationId());
        assertEquals("Campus Cafeteria", response.getOrganizationName());

        verify(passwordEncoder, times(1)).encode("SecurePass@123");
        verify(organizationRepository, times(1)).save(any(Organization.class));
        verify(userRepository, times(1)).save(any(User.class));
        verify(auditService, times(1)).log(eq(201L), eq("USER_REGISTERED"), eq("USER"), eq("201"), anyString());
    }

    @Test
    @DisplayName("Should reject registration when email already exists")
    void register_duplicateEmail() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("existing@org.com");
        req.setPassword("Password@123");
        req.setRole("RECIPIENT");
        req.setOrganizationName("Hope NGO");

        User existing = User.builder().userId(1L).email("existing@org.com").build();
        when(userRepository.findByEmail("existing@org.com")).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
        verify(organizationRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject registration if role is ADMIN (public cannot register as ADMIN)")
    void register_invalidRole() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("hacker@dark.net");
        req.setPassword("Password@123");
        req.setRole("ADMIN"); // Not allowed
        req.setOrganizationName("Malicious Org");

        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    @DisplayName("Should return current user details with organization profile")
    void getCurrentUser_success() {
        Long userId = 10L;
        Long orgId = 5L;

        User user = User.builder()
                .userId(userId)
                .email("user@vit.ac.in")
                .fullName("Prof. John")
                .role("PROVIDER")
                .organizationId(orgId)
                .build();

        Organization org = Organization.builder()
                .organizationId(orgId)
                .organizationName("VIT Caterers")
                .verificationStatus("APPROVED")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));

        AuthResponse res = authService.getCurrentUser(userId);

        assertNotNull(res);
        assertEquals("user@vit.ac.in", res.getEmail());
        assertEquals("VIT Caterers", res.getOrganizationName());
        assertEquals("APPROVED", res.getOrganizationStatus());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user does not exist")
    void getCurrentUser_notFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.getCurrentUser(999L));
    }
}

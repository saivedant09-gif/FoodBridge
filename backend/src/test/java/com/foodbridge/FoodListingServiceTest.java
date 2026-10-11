package com.foodbridge;

import com.foodbridge.dto.FoodListingRequest;
import com.foodbridge.dto.PageResponse;
import com.foodbridge.exception.UnauthorizedActionException;
import com.foodbridge.model.FoodListing;
import com.foodbridge.model.Organization;
import com.foodbridge.model.User;
import com.foodbridge.repository.FoodListingRepository;
import com.foodbridge.repository.OrganizationRepository;
import com.foodbridge.repository.UserRepository;
import com.foodbridge.service.AuditService;
import com.foodbridge.service.FoodListingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FoodListingService verifying:
 * - Listing creation with date/quantity validations
 * - Filtering active listings
 * - Provider ownership checks on update/cancel
 * - Expiration handling
 */
@ExtendWith(MockitoExtension.class)
class FoodListingServiceTest {

    @Mock private FoodListingRepository foodListingRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;

    private FoodListingService foodListingService;

    @BeforeEach
    void setUp() {
        foodListingService = new FoodListingService(
                foodListingRepository,
                organizationRepository,
                userRepository,
                auditService
        );
    }

    @Test
    @DisplayName("Should create listing successfully when input is valid and provider organization is approved")
    void createListing_success() {
        Long userId = 10L;
        Long orgId = 1L;

        FoodListingRequest req = new FoodListingRequest();
        req.setFoodName("Vegetable Pulao");
        req.setCategory("COOKED_MEAL");
        req.setTotalQuantity(new BigDecimal("30.00"));
        req.setUnit("portions");
        req.setExpiresAt(LocalDateTime.now().plusHours(6).toString());
        req.setPickupDeadline(LocalDateTime.now().plusHours(4).toString());
        req.setPickupAddress("VIT Main Canteen");

        User user = User.builder().userId(userId).organizationId(orgId).role("PROVIDER").build();
        Organization org = Organization.builder().organizationId(orgId).verificationStatus("APPROVED").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(foodListingRepository.save(any(FoodListing.class))).thenReturn(501L);

        FoodListing created = foodListingService.create(req, userId);

        assertNotNull(created);
        assertEquals(501L, created.getListingId());
        assertEquals("Vegetable Pulao", created.getFoodName());
        assertEquals("ACTIVE", created.getListingStatus());
        verify(foodListingRepository, times(1)).save(any(FoodListing.class));
        verify(auditService, times(1)).log(eq(userId), eq("FOOD_LISTING_CREATED"), eq("FOOD_LISTING"), eq("501"), anyString());
    }

    @Test
    @DisplayName("Should reject listing creation when total quantity is zero or negative")
    void createListing_zeroQuantity() {
        Long userId = 10L;
        FoodListingRequest req = new FoodListingRequest();
        req.setFoodName("Bread");
        req.setTotalQuantity(BigDecimal.ZERO);
        req.setUnit("loaves");

        assertThrows(IllegalArgumentException.class, () -> foodListingService.create(req, userId));
        verify(foodListingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return paginated active listings with proper metadata")
    void getActive_returnsPagedResults() {
        FoodListing item = FoodListing.builder()
                .listingId(1L)
                .foodName("Dal Makhani")
                .availableQuantity(new BigDecimal("20.00"))
                .unit("portions")
                .listingStatus("ACTIVE")
                .build();

        when(foodListingRepository.findActive("COOKED_MEAL", "Vellore", 0, 10))
                .thenReturn(List.of(item));
        when(foodListingRepository.countByStatus("ACTIVE")).thenReturn(1L);

        PageResponse<FoodListing> response = foodListingService.getActive("COOKED_MEAL", "Vellore", 0, 10);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals("Dal Makhani", response.getContent().get(0).getFoodName());
        assertEquals(1L, response.getTotalElements());
    }

    @Test
    @DisplayName("Should cancel listing when owned by provider")
    void cancelListing_success() {
        Long listingId = 200L;
        Long userId = 10L;
        Long orgId = 1L;

        User user = User.builder().userId(userId).organizationId(orgId).role("PROVIDER").build();
        FoodListing listing = FoodListing.builder()
                .listingId(listingId)
                .providerOrganizationId(orgId)
                .listingStatus("ACTIVE")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(foodListingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        foodListingService.cancel(listingId, userId);

        verify(foodListingRepository, times(1)).updateStatus(listingId, "CANCELLED");
        verify(auditService, times(1)).log(eq(userId), eq("FOOD_LISTING_CANCELLED"), eq("FOOD_LISTING"), eq("200"), anyString());
    }

    @Test
    @DisplayName("Should reject cancellation when user does not own the listing")
    void cancelListing_unauthorized() {
        Long listingId = 200L;
        Long userId = 10L;

        User user = User.builder().userId(userId).organizationId(1L).role("PROVIDER").build();
        FoodListing listing = FoodListing.builder()
                .listingId(listingId)
                .providerOrganizationId(999L) // belongs to different organization
                .listingStatus("ACTIVE")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(foodListingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        assertThrows(UnauthorizedActionException.class, () -> foodListingService.cancel(listingId, userId));
        verify(foodListingRepository, never()).updateStatus(any(), any());
    }
}

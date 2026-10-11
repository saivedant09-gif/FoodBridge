package com.foodbridge;

import com.foodbridge.exception.InsufficientFoodException;
import com.foodbridge.exception.ResourceNotFoundException;
import com.foodbridge.exception.UnauthorizedActionException;
import com.foodbridge.model.*;
import com.foodbridge.repository.*;
import com.foodbridge.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AllocationService verifying:
 * - Transaction-safe allocation workflow
 * - Concurrency protection against overselling (conditional decrement check)
 * - Role-based authorization & ownership enforcement
 * - State transition validation
 * - Allocation cancellation and inventory restoration
 */
@ExtendWith(MockitoExtension.class)
class AllocationServiceTest {

    @Mock private AllocationRepository allocationRepository;
    @Mock private RequestRepository requestRepository;
    @Mock private FoodListingRepository foodListingRepository;
    @Mock private PickupRepository pickupRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;

    private AllocationService allocationService;

    @BeforeEach
    void setUp() {
        allocationService = new AllocationService(
                allocationRepository,
                requestRepository,
                foodListingRepository,
                pickupRepository,
                userRepository,
                notificationService,
                auditService
        );
    }

    @Test
    @DisplayName("Should approve request and create allocation when sufficient inventory exists")
    void approveRequest_success() {
        Long requestId = 100L;
        Long providerUserId = 10L;
        Long listingId = 50L;
        Long providerOrgId = 1L;
        Long recipientOrgId = 2L;

        FoodRequest request = FoodRequest.builder()
                .requestId(requestId)
                .listingId(listingId)
                .recipientOrganizationId(recipientOrgId)
                .requestedQuantity(new BigDecimal("25.00"))
                .requestStatus("PENDING")
                .build();

        FoodListing listing = FoodListing.builder()
                .listingId(listingId)
                .providerOrganizationId(providerOrgId)
                .foodName("Cooked Rice")
                .totalQuantity(new BigDecimal("50.00"))
                .availableQuantity(new BigDecimal("50.00"))
                .unit("portions")
                .listingStatus("ACTIVE")
                .pickupDeadline(LocalDateTime.now().plusHours(4))
                .build();

        User providerUser = User.builder()
                .userId(providerUserId)
                .organizationId(providerOrgId)
                .role("PROVIDER")
                .build();

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(foodListingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(userRepository.findById(providerUserId)).thenReturn(Optional.of(providerUser));
        when(foodListingRepository.decrementAvailableQuantity(listingId, new BigDecimal("25.00"))).thenReturn(1);
        when(allocationRepository.save(any(Allocation.class))).thenReturn(1001L);
        when(userRepository.findByOrganizationId(recipientOrgId)).thenReturn(Collections.emptyList());

        Allocation result = allocationService.approve(requestId, providerUserId);

        assertNotNull(result);
        assertEquals(1001L, result.getAllocationId());
        assertEquals("CONFIRMED", result.getAllocationStatus());
        assertEquals(new BigDecimal("25.00"), result.getAllocatedQuantity());

        verify(foodListingRepository, times(1)).decrementAvailableQuantity(listingId, new BigDecimal("25.00"));
        verify(requestRepository, times(1)).updateStatus(requestId, "APPROVED", null);
        verify(pickupRepository, times(1)).save(any(CollectionRecord.class));
        verify(auditService, times(1)).log(eq(providerUserId), eq("ALLOCATION_CONFIRMED"), eq("ALLOCATION"), anyString(), anyString());
    }

    @Test
    @DisplayName("Should throw InsufficientFoodException when conditional decrement returns 0 (overselling prevention)")
    void approveRequest_insufficientQuantity() {
        Long requestId = 100L;
        Long providerUserId = 10L;
        Long listingId = 50L;
        Long providerOrgId = 1L;

        FoodRequest request = FoodRequest.builder()
                .requestId(requestId)
                .listingId(listingId)
                .recipientOrganizationId(2L)
                .requestedQuantity(new BigDecimal("60.00"))
                .requestStatus("PENDING")
                .build();

        FoodListing listing = FoodListing.builder()
                .listingId(listingId)
                .providerOrganizationId(providerOrgId)
                .foodName("Cooked Rice")
                .totalQuantity(new BigDecimal("50.00"))
                .availableQuantity(new BigDecimal("10.00"))
                .unit("portions")
                .listingStatus("ACTIVE")
                .build();

        User providerUser = User.builder()
                .userId(providerUserId)
                .organizationId(providerOrgId)
                .role("PROVIDER")
                .build();

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(foodListingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(userRepository.findById(providerUserId)).thenReturn(Optional.of(providerUser));
        // Atomic decrement fails because available_quantity < requested
        when(foodListingRepository.decrementAvailableQuantity(listingId, new BigDecimal("60.00"))).thenReturn(0);

        assertThrows(InsufficientFoodException.class, () -> allocationService.approve(requestId, providerUserId));

        verify(allocationRepository, never()).save(any());
        verify(requestRepository, never()).updateStatus(any(), any(), any());
    }

    @Test
    @DisplayName("Should reject approval when food request is not in PENDING status")
    void approveRequest_requestNotPending() {
        Long requestId = 100L;
        Long providerUserId = 10L;

        FoodRequest request = FoodRequest.builder()
                .requestId(requestId)
                .listingId(50L)
                .requestedQuantity(new BigDecimal("10.00"))
                .requestStatus("APPROVED")
                .build();

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));

        assertThrows(IllegalStateException.class, () -> allocationService.approve(requestId, providerUserId));
        verify(foodListingRepository, never()).decrementAvailableQuantity(any(), any());
    }

    @Test
    @DisplayName("Should reject approval when caller is not the owner provider of the listing")
    void approveRequest_unauthorizedProvider() {
        Long requestId = 100L;
        Long providerUserId = 10L;
        Long listingId = 50L;

        FoodRequest request = FoodRequest.builder()
                .requestId(requestId)
                .listingId(listingId)
                .requestedQuantity(new BigDecimal("10.00"))
                .requestStatus("PENDING")
                .build();

        FoodListing listing = FoodListing.builder()
                .listingId(listingId)
                .providerOrganizationId(999L) // belongs to org 999
                .listingStatus("ACTIVE")
                .build();

        User providerUser = User.builder()
                .userId(providerUserId)
                .organizationId(888L) // caller belongs to org 888
                .role("PROVIDER")
                .build();

        when(requestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(foodListingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(userRepository.findById(providerUserId)).thenReturn(Optional.of(providerUser));

        assertThrows(UnauthorizedActionException.class, () -> allocationService.approve(requestId, providerUserId));
        verify(foodListingRepository, never()).decrementAvailableQuantity(any(), any());
    }

    @Test
    @DisplayName("Should cancel confirmed allocation and restore inventory")
    void cancelAllocation_success() {
        Long allocationId = 500L;
        Long userId = 20L;
        Long orgId = 2L;

        Allocation allocation = Allocation.builder()
                .allocationId(allocationId)
                .listingId(50L)
                .requestId(100L)
                .recipientOrganizationId(orgId)
                .allocatedQuantity(new BigDecimal("20.00"))
                .allocationStatus("CONFIRMED")
                .build();

        User recipientUser = User.builder()
                .userId(userId)
                .organizationId(orgId)
                .role("RECIPIENT")
                .build();

        CollectionRecord collectionRecord = CollectionRecord.builder()
                .collectionId(700L)
                .allocationId(allocationId)
                .build();

        FoodListing listing = FoodListing.builder().listingId(50L).providerOrganizationId(1L).build();
        when(allocationRepository.findById(allocationId)).thenReturn(Optional.of(allocation));
        when(userRepository.findById(userId)).thenReturn(Optional.of(recipientUser));
        when(foodListingRepository.findById(50L)).thenReturn(Optional.of(listing));
        when(pickupRepository.findByAllocationId(allocationId)).thenReturn(Optional.of(collectionRecord));

        allocationService.cancel(allocationId, "Cannot collect in time", userId);

        verify(allocationRepository, times(1)).cancel(allocationId, "Cannot collect in time");
        verify(foodListingRepository, times(1)).incrementAvailableQuantity(50L, new BigDecimal("20.00"));
        verify(requestRepository, times(1)).updateStatus(100L, "CANCELLED", "Allocation cancelled: Cannot collect in time");
        verify(pickupRepository, times(1)).updateStatus(700L, "CANCELLED");
    }
}

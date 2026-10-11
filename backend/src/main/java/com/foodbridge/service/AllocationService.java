package com.foodbridge.service;

import com.foodbridge.dto.PageResponse;
import com.foodbridge.exception.InsufficientFoodException;
import com.foodbridge.exception.ResourceNotFoundException;
import com.foodbridge.exception.UnauthorizedActionException;
import com.foodbridge.model.Allocation;
import com.foodbridge.model.CollectionRecord;
import com.foodbridge.model.FoodListing;
import com.foodbridge.model.FoodRequest;
import com.foodbridge.model.User;
import com.foodbridge.repository.AllocationRepository;
import com.foodbridge.repository.FoodListingRepository;
import com.foodbridge.repository.PickupRepository;
import com.foodbridge.repository.RequestRepository;
import com.foodbridge.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AllocationService {

    private final AllocationRepository allocationRepository;
    private final RequestRepository requestRepository;
    private final FoodListingRepository foodListingRepository;
    private final PickupRepository pickupRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public AllocationService(AllocationRepository allocationRepository,
                             RequestRepository requestRepository,
                             FoodListingRepository foodListingRepository,
                             PickupRepository pickupRepository,
                             UserRepository userRepository,
                             NotificationService notificationService,
                             AuditService auditService) {
        this.allocationRepository = allocationRepository;
        this.requestRepository = requestRepository;
        this.foodListingRepository = foodListingRepository;
        this.pickupRepository = pickupRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Allocation approve(Long requestId, Long providerUserId) {
        if (requestId == null || providerUserId == null) {
            throw new IllegalArgumentException("Request ID and provider user ID must not be null");
        }

        FoodRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Food request not found with id: " + requestId));

        if (!"PENDING".equalsIgnoreCase(request.getRequestStatus())) {
            throw new IllegalStateException("Food request is not in PENDING status. Current status: " + request.getRequestStatus());
        }

        FoodListing listing = foodListingRepository.findById(request.getListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Food listing not found with id: " + request.getListingId()));

        User user = userRepository.findById(providerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider user not found with id: " + providerUserId));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole()) || "ROLE_ADMIN".equalsIgnoreCase(user.getRole());
        boolean isOwner = listing.getProviderOrganizationId().equals(user.getOrganizationId());

        if (!isAdmin && !isOwner) {
            throw new UnauthorizedActionException("You are not authorized to approve requests for this listing");
        }

        if (!"ACTIVE".equalsIgnoreCase(listing.getListingStatus())) {
            throw new IllegalStateException("Food listing is not active. Current status: " + listing.getListingStatus());
        }

        int rowsUpdated = foodListingRepository.decrementAvailableQuantity(listing.getListingId(), request.getRequestedQuantity());
        if (rowsUpdated == 0) {
            throw new InsufficientFoodException("Insufficient food quantity available to fulfill request (" +
                    request.getRequestedQuantity() + " " + listing.getUnit() + ")");
        }

        Allocation allocation = Allocation.builder()
                .requestId(request.getRequestId())
                .listingId(listing.getListingId())
                .recipientOrganizationId(request.getRecipientOrganizationId())
                .allocatedQuantity(request.getRequestedQuantity())
                .allocationStatus("CONFIRMED")
                .confirmedAt(LocalDateTime.now())
                .foodName(listing.getFoodName())
                .unit(listing.getUnit())
                .build();

        Long allocationId = allocationRepository.save(allocation);
        allocation.setAllocationId(allocationId);

        requestRepository.updateStatus(requestId, "APPROVED", null);

        CollectionRecord collectionRecord = CollectionRecord.builder()
                .allocationId(allocationId)
                .pickupDeadline(listing.getPickupDeadline())
                .collectionStatus("SCHEDULED")
                .providerConfirmed(false)
                .recipientConfirmed(false)
                .build();
        pickupRepository.save(collectionRecord);

        List<User> recipientUsers = userRepository.findByOrganizationId(request.getRecipientOrganizationId());
        for (User recipientUser : recipientUsers) {
            notificationService.notifyUser(
                    recipientUser.getUserId(),
                    "Food Request Approved",
                    "Your request for " + request.getRequestedQuantity() + " " + listing.getUnit() +
                            " of '" + listing.getFoodName() + "' has been approved. Collection is scheduled.",
                    "SUCCESS",
                    "ALLOCATION",
                    allocationId
            );
        }

        auditService.log(providerUserId, "ALLOCATION_CONFIRMED", "ALLOCATION",
                String.valueOf(allocationId), "Allocation confirmed for " + request.getRequestedQuantity() + " " + listing.getUnit());

        return allocation;
    }

    public Allocation getById(Long allocationId, Long userId) {
        if (allocationId == null || userId == null) {
            throw new IllegalArgumentException("Allocation ID and user ID cannot be null");
        }

        Allocation allocation = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation not found with id: " + allocationId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole()) || "ROLE_ADMIN".equalsIgnoreCase(user.getRole());
        boolean isRecipient = allocation.getRecipientOrganizationId().equals(user.getOrganizationId());

        boolean isProvider = false;
        FoodListing listing = foodListingRepository.findById(allocation.getListingId()).orElse(null);
        if (listing != null && listing.getProviderOrganizationId().equals(user.getOrganizationId())) {
            isProvider = true;
        }

        if (!isAdmin && !isRecipient && !isProvider) {
            throw new UnauthorizedActionException("You are not authorized to view this allocation");
        }

        return allocation;
    }

    public PageResponse<Allocation> getMyAllocations(Long userId, int page, int size) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (size <= 0) {
            size = 10;
        }
        if (page < 0) {
            page = 0;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getOrganizationId() == null) {
            throw new ResourceNotFoundException("User is not associated with an organization");
        }

        Long orgId = user.getOrganizationId();
        List<Allocation> content = allocationRepository.findByRecipientOrgId(orgId, page, size);
        long totalElements = content.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean last = (page + 1) >= totalPages;

        return PageResponse.<Allocation>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .last(last)
                .build();
    }

    @Transactional
    public void cancel(Long allocationId, String reason, Long userId) {
        if (allocationId == null || userId == null) {
            throw new IllegalArgumentException("Allocation ID and user ID cannot be null");
        }

        Allocation allocation = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation not found with id: " + allocationId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        FoodListing listing = foodListingRepository.findById(allocation.getListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Food listing not found with id: " + allocation.getListingId()));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole()) || "ROLE_ADMIN".equalsIgnoreCase(user.getRole());
        boolean isRecipient = allocation.getRecipientOrganizationId().equals(user.getOrganizationId());
        boolean isProvider = listing.getProviderOrganizationId().equals(user.getOrganizationId());

        if (!isAdmin && !isRecipient && !isProvider) {
            throw new UnauthorizedActionException("You are not authorized to cancel this allocation");
        }

        if (!"CONFIRMED".equalsIgnoreCase(allocation.getAllocationStatus())) {
            throw new IllegalStateException("Only CONFIRMED allocations can be cancelled. Current status: " + allocation.getAllocationStatus());
        }

        String cancellationReason = (reason != null && !reason.isBlank()) ? reason.trim() : "Cancelled by user";

        allocationRepository.cancel(allocationId, cancellationReason);
        foodListingRepository.incrementAvailableQuantity(allocation.getListingId(), allocation.getAllocatedQuantity());
        requestRepository.updateStatus(allocation.getRequestId(), "CANCELLED", "Allocation cancelled: " + cancellationReason);

        pickupRepository.findByAllocationId(allocationId).ifPresent(col ->
                pickupRepository.updateStatus(col.getCollectionId(), "CANCELLED"));

        auditService.log(userId, "ALLOCATION_CANCELLED", "ALLOCATION",
                String.valueOf(allocationId), "Allocation cancelled: " + cancellationReason);

        // Notify counterpart
        Long notifyOrgId = isProvider ? allocation.getRecipientOrganizationId() : listing.getProviderOrganizationId();
        List<User> targetUsers = userRepository.findByOrganizationId(notifyOrgId);
        for (User targetUser : targetUsers) {
            notificationService.notifyUser(
                    targetUser.getUserId(),
                    "Allocation Cancelled",
                    "Allocation for '" + listing.getFoodName() + "' was cancelled. Reason: " + cancellationReason,
                    "WARNING",
                    "ALLOCATION",
                    allocationId
            );
        }
    }
}

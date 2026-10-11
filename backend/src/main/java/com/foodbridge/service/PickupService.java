package com.foodbridge.service;

import com.foodbridge.dto.PageResponse;
import com.foodbridge.exception.ResourceNotFoundException;
import com.foodbridge.exception.UnauthorizedActionException;
import com.foodbridge.model.Allocation;
import com.foodbridge.model.CollectionRecord;
import com.foodbridge.model.FoodListing;
import com.foodbridge.model.User;
import com.foodbridge.repository.AllocationRepository;
import com.foodbridge.repository.FoodListingRepository;
import com.foodbridge.repository.PickupRepository;
import com.foodbridge.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PickupService {

    private final PickupRepository pickupRepository;
    private final AllocationRepository allocationRepository;
    private final FoodListingRepository foodListingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public PickupService(PickupRepository pickupRepository,
                         AllocationRepository allocationRepository,
                         FoodListingRepository foodListingRepository,
                         UserRepository userRepository,
                         NotificationService notificationService,
                         AuditService auditService) {
        this.pickupRepository = pickupRepository;
        this.allocationRepository = allocationRepository;
        this.foodListingRepository = foodListingRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    public PageResponse<CollectionRecord> getMyCollections(Long userId, String role, int page, int size) {
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
        String effectiveRole = (role != null && !role.isBlank()) ? role.trim().toUpperCase() : user.getRole();

        List<CollectionRecord> content;
        if ("PROVIDER".equalsIgnoreCase(effectiveRole)) {
            content = pickupRepository.findByProviderOrgId(orgId, page, size);
        } else {
            content = pickupRepository.findByRecipientOrgId(orgId, page, size);
        }

        long totalElements = content.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean last = (page + 1) >= totalPages;

        return PageResponse.<CollectionRecord>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .last(last)
                .build();
    }

    public CollectionRecord getById(Long collectionId, Long userId, String role) {
        if (collectionId == null || userId == null) {
            throw new IllegalArgumentException("Collection ID and user ID cannot be null");
        }

        CollectionRecord record = pickupRepository.findById(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection record not found with id: " + collectionId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Allocation allocation = allocationRepository.findById(record.getAllocationId())
                .orElseThrow(() -> new ResourceNotFoundException("Allocation not found with id: " + record.getAllocationId()));

        FoodListing listing = foodListingRepository.findById(allocation.getListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Food listing not found with id: " + allocation.getListingId()));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole()) || "ROLE_ADMIN".equalsIgnoreCase(user.getRole());
        boolean isRecipient = allocation.getRecipientOrganizationId().equals(user.getOrganizationId());
        boolean isProvider = listing.getProviderOrganizationId().equals(user.getOrganizationId());

        if (!isAdmin && !isRecipient && !isProvider) {
            throw new UnauthorizedActionException("You are not authorized to view this collection record");
        }

        return record;
    }

    @Transactional
    public CollectionRecord confirmProvider(Long collectionId, Long providerUserId) {
        if (collectionId == null || providerUserId == null) {
            throw new IllegalArgumentException("Collection ID and provider user ID cannot be null");
        }

        CollectionRecord record = pickupRepository.findById(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection record not found with id: " + collectionId));

        Allocation allocation = allocationRepository.findById(record.getAllocationId())
                .orElseThrow(() -> new ResourceNotFoundException("Allocation not found with id: " + record.getAllocationId()));

        FoodListing listing = foodListingRepository.findById(allocation.getListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Food listing not found with id: " + allocation.getListingId()));

        User user = userRepository.findById(providerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider user not found with id: " + providerUserId));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole()) || "ROLE_ADMIN".equalsIgnoreCase(user.getRole());
        boolean isProvider = listing.getProviderOrganizationId().equals(user.getOrganizationId());

        if (!isAdmin && !isProvider) {
            throw new UnauthorizedActionException("You are not authorized to confirm this pickup as provider");
        }

        pickupRepository.confirmProvider(collectionId);

        if (Boolean.TRUE.equals(record.getRecipientConfirmed())) {
            pickupRepository.markCollected(collectionId);
            allocationRepository.updateStatus(allocation.getAllocationId(), "COMPLETED");
        }

        auditService.log(providerUserId, "COLLECTION_PROVIDER_CONFIRMED", "COLLECTION",
                String.valueOf(collectionId), "Provider confirmed collection handover");

        List<User> recipientUsers = userRepository.findByOrganizationId(allocation.getRecipientOrganizationId());
        for (User recipientUser : recipientUsers) {
            notificationService.notifyUser(
                    recipientUser.getUserId(),
                    "Pickup Confirmed by Provider",
                    "Provider has confirmed handover for '" + listing.getFoodName() + "'.",
                    "INFO",
                    "COLLECTION",
                    collectionId
            );
        }

        return pickupRepository.findById(collectionId).orElse(record);
    }

    @Transactional
    public CollectionRecord confirmRecipient(Long collectionId, Long recipientUserId) {
        if (collectionId == null || recipientUserId == null) {
            throw new IllegalArgumentException("Collection ID and recipient user ID must not be null");
        }

        CollectionRecord record = pickupRepository.findById(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection record not found with id: " + collectionId));

        Allocation allocation = allocationRepository.findById(record.getAllocationId())
                .orElseThrow(() -> new ResourceNotFoundException("Allocation not found with id: " + record.getAllocationId()));

        FoodListing listing = foodListingRepository.findById(allocation.getListingId())
                .orElseThrow(() -> new ResourceNotFoundException("Food listing not found with id: " + allocation.getListingId()));

        User user = userRepository.findById(recipientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipient user not found with id: " + recipientUserId));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole()) || "ROLE_ADMIN".equalsIgnoreCase(user.getRole());
        boolean isRecipient = allocation.getRecipientOrganizationId().equals(user.getOrganizationId());

        if (!isAdmin && !isRecipient) {
            throw new UnauthorizedActionException("You are not authorized to confirm this pickup as recipient");
        }

        pickupRepository.confirmRecipient(collectionId);

        if (Boolean.TRUE.equals(record.getProviderConfirmed())) {
            pickupRepository.markCollected(collectionId);
            allocationRepository.updateStatus(allocation.getAllocationId(), "COMPLETED");
        }

        auditService.log(recipientUserId, "COLLECTION_RECIPIENT_CONFIRMED", "COLLECTION",
                String.valueOf(collectionId), "Recipient confirmed collection receipt");

        List<User> providerUsers = userRepository.findByOrganizationId(listing.getProviderOrganizationId());
        for (User providerUser : providerUsers) {
            notificationService.notifyUser(
                    providerUser.getUserId(),
                    "Pickup Confirmed by Recipient",
                    "Recipient has confirmed receipt for '" + listing.getFoodName() + "'.",
                    "SUCCESS",
                    "COLLECTION",
                    collectionId
            );
        }

        return pickupRepository.findById(collectionId).orElse(record);
    }
}

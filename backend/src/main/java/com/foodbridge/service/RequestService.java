package com.foodbridge.service;

import com.foodbridge.dto.PageResponse;
import com.foodbridge.exception.InsufficientFoodException;
import com.foodbridge.exception.ResourceNotFoundException;
import com.foodbridge.exception.UnauthorizedActionException;
import com.foodbridge.model.FoodListing;
import com.foodbridge.model.FoodRequest;
import com.foodbridge.model.Organization;
import com.foodbridge.model.User;
import com.foodbridge.repository.FoodListingRepository;
import com.foodbridge.repository.OrganizationRepository;
import com.foodbridge.repository.RequestRepository;
import com.foodbridge.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RequestService {

    private final RequestRepository requestRepository;
    private final FoodListingRepository foodListingRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public RequestService(RequestRepository requestRepository,
                          FoodListingRepository foodListingRepository,
                          OrganizationRepository organizationRepository,
                          UserRepository userRepository,
                          NotificationService notificationService,
                          AuditService auditService) {
        this.requestRepository = requestRepository;
        this.foodListingRepository = foodListingRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Transactional
    public FoodRequest submit(Long listingId, BigDecimal qty, String message, Long recipientUserId) {
        if (listingId == null || qty == null || recipientUserId == null) {
            throw new IllegalArgumentException("Listing ID, requested quantity, and recipient user ID must not be null");
        }
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Requested quantity must be greater than zero");
        }

        FoodListing listing = foodListingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Food listing not found with id: " + listingId));

        if (!"ACTIVE".equalsIgnoreCase(listing.getListingStatus())) {
            throw new IllegalStateException("Food listing is not active. Current status: " + listing.getListingStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        if ((listing.getExpiresAt() != null && listing.getExpiresAt().isBefore(now)) ||
            (listing.getPickupDeadline() != null && listing.getPickupDeadline().isBefore(now))) {
            throw new IllegalStateException("Food listing has already expired or passed its pickup deadline");
        }

        if (listing.getAvailableQuantity() == null || listing.getAvailableQuantity().compareTo(qty) < 0) {
            throw new InsufficientFoodException("Requested quantity (" + qty + ") exceeds available quantity (" +
                    (listing.getAvailableQuantity() != null ? listing.getAvailableQuantity() : 0) + ")");
        }

        User user = userRepository.findById(recipientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + recipientUserId));

        if (user.getOrganizationId() == null) {
            throw new UnauthorizedActionException("User is not associated with an organization");
        }

        Organization org = organizationRepository.findById(user.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + user.getOrganizationId()));

        if (!"APPROVED".equalsIgnoreCase(org.getVerificationStatus())) {
            throw new UnauthorizedActionException("Recipient organization must be APPROVED to request food. Current status: " + org.getVerificationStatus());
        }

        if (org.getOrganizationId().equals(listing.getProviderOrganizationId())) {
            throw new IllegalArgumentException("Provider organization cannot request its own food listing");
        }

        if (requestRepository.existsActiveRequestForListingByOrg(listingId, org.getOrganizationId())) {
            throw new IllegalStateException("An active request already exists for this listing from your organization");
        }

        FoodRequest foodRequest = FoodRequest.builder()
                .listingId(listingId)
                .recipientOrganizationId(org.getOrganizationId())
                .requestedQuantity(qty)
                .requestStatus("PENDING")
                .message(message)
                .foodName(listing.getFoodName())
                .recipientName(org.getOrganizationName())
                .unit(listing.getUnit())
                .build();

        Long requestId = requestRepository.save(foodRequest);
        foodRequest.setRequestId(requestId);

        List<User> providerUsers = userRepository.findByOrganizationId(listing.getProviderOrganizationId());
        for (User providerUser : providerUsers) {
            notificationService.notifyUser(
                    providerUser.getUserId(),
                    "New Food Request",
                    org.getOrganizationName() + " requested " + qty + " " + listing.getUnit() + " of '" + listing.getFoodName() + "'",
                    "REQUEST",
                    "FOOD_REQUEST",
                    requestId
            );
        }

        auditService.log(recipientUserId, "FOOD_REQUEST_SUBMITTED", "FOOD_REQUEST",
                String.valueOf(requestId), "Requested " + qty + " of listing " + listingId);

        return foodRequest;
    }

    public PageResponse<FoodRequest> getMyRequests(Long recipientUserId, int page, int size) {
        if (recipientUserId == null) {
            throw new IllegalArgumentException("Recipient user ID cannot be null");
        }
        if (size <= 0) {
            size = 10;
        }
        if (page < 0) {
            page = 0;
        }

        User user = userRepository.findById(recipientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + recipientUserId));

        if (user.getOrganizationId() == null) {
            throw new ResourceNotFoundException("User is not associated with an organization");
        }

        Long orgId = user.getOrganizationId();
        List<FoodRequest> content = requestRepository.findByRecipientOrgId(orgId, page, size);
        long totalElements = content.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean last = (page + 1) >= totalPages;

        return PageResponse.<FoodRequest>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .last(last)
                .build();
    }

    public PageResponse<FoodRequest> getPendingForProvider(Long providerUserId, int page, int size) {
        if (providerUserId == null) {
            throw new IllegalArgumentException("Provider user ID cannot be null");
        }
        if (size <= 0) {
            size = 10;
        }
        if (page < 0) {
            page = 0;
        }

        User user = userRepository.findById(providerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + providerUserId));

        if (user.getOrganizationId() == null) {
            throw new ResourceNotFoundException("User is not associated with an organization");
        }

        Long orgId = user.getOrganizationId();
        List<FoodRequest> content = requestRepository.findPendingByProviderOrgId(orgId, page, size);
        long totalElements = requestRepository.countPendingByProviderOrgId(orgId);
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean last = (page + 1) >= totalPages;

        return PageResponse.<FoodRequest>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .last(last)
                .build();
    }

    public FoodRequest getById(Long requestId, Long userId) {
        if (requestId == null || userId == null) {
            throw new IllegalArgumentException("Request ID and user ID cannot be null");
        }

        FoodRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Food request not found with id: " + requestId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole()) || "ROLE_ADMIN".equalsIgnoreCase(user.getRole());
        boolean isRecipientOwner = req.getRecipientOrganizationId().equals(user.getOrganizationId());

        boolean isProviderOwner = false;
        FoodListing listing = foodListingRepository.findById(req.getListingId()).orElse(null);
        if (listing != null && listing.getProviderOrganizationId().equals(user.getOrganizationId())) {
            isProviderOwner = true;
        }

        if (!isAdmin && !isRecipientOwner && !isProviderOwner) {
            throw new UnauthorizedActionException("You are not authorized to view this food request");
        }

        return req;
    }

    @Transactional
    public void cancel(Long requestId, Long recipientUserId) {
        if (requestId == null || recipientUserId == null) {
            throw new IllegalArgumentException("Request ID and user ID cannot be null");
        }

        FoodRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Food request not found with id: " + requestId));

        User user = userRepository.findById(recipientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + recipientUserId));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole()) || "ROLE_ADMIN".equalsIgnoreCase(user.getRole());
        boolean isRecipientOwner = req.getRecipientOrganizationId().equals(user.getOrganizationId());

        if (!isAdmin && !isRecipientOwner) {
            throw new UnauthorizedActionException("You are not authorized to cancel this food request");
        }

        if (!"PENDING".equalsIgnoreCase(req.getRequestStatus())) {
            throw new IllegalStateException("Only PENDING requests can be cancelled. Current status: " + req.getRequestStatus());
        }

        requestRepository.updateStatus(requestId, "CANCELLED", "Cancelled by requester");

        auditService.log(recipientUserId, "FOOD_REQUEST_CANCELLED", "FOOD_REQUEST",
                String.valueOf(requestId), "Food request cancelled by recipient");
    }
}

package com.foodbridge.service;

import com.foodbridge.dto.FoodListingRequest;
import com.foodbridge.dto.PageResponse;
import com.foodbridge.exception.ResourceNotFoundException;
import com.foodbridge.exception.UnauthorizedActionException;
import com.foodbridge.model.FoodListing;
import com.foodbridge.model.Organization;
import com.foodbridge.model.User;
import com.foodbridge.repository.FoodListingRepository;
import com.foodbridge.repository.OrganizationRepository;
import com.foodbridge.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class FoodListingService {

    private final FoodListingRepository foodListingRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public FoodListingService(FoodListingRepository foodListingRepository,
                              OrganizationRepository organizationRepository,
                              UserRepository userRepository,
                              AuditService auditService) {
        this.foodListingRepository = foodListingRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public FoodListing create(FoodListingRequest req, Long providerUserId) {
        if (req == null) {
            throw new IllegalArgumentException("Food listing request cannot be null");
        }
        if (providerUserId == null) {
            throw new IllegalArgumentException("Provider user ID cannot be null");
        }
        if (req.getFoodName() == null || req.getFoodName().isBlank()) {
            throw new IllegalArgumentException("Food name cannot be blank");
        }
        if (req.getTotalQuantity() == null || req.getTotalQuantity().signum() <= 0) {
            throw new IllegalArgumentException("Total quantity must be greater than zero");
        }
        if (req.getUnit() == null || req.getUnit().isBlank()) {
            throw new IllegalArgumentException("Unit of measurement cannot be blank");
        }

        User user = userRepository.findById(providerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + providerUserId));

        if (user.getOrganizationId() == null) {
            throw new UnauthorizedActionException("User is not associated with any organization");
        }

        Organization org = organizationRepository.findById(user.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + user.getOrganizationId()));

        if (!"APPROVED".equalsIgnoreCase(org.getVerificationStatus())) {
            throw new UnauthorizedActionException("Organization must be APPROVED to publish food listings. Current status: " + org.getVerificationStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = parseDateTime(req.getExpiresAt());
        LocalDateTime pickupDeadline = parseDateTime(req.getPickupDeadline());
        LocalDateTime preparedAt = parseDateTime(req.getPreparedAt());

        if (expiresAt == null || expiresAt.isBefore(now)) {
            throw new IllegalArgumentException("Expiration datetime must be in the future");
        }
        if (pickupDeadline == null || pickupDeadline.isBefore(now)) {
            throw new IllegalArgumentException("Pickup deadline must be in the future");
        }
        if (pickupDeadline.isAfter(expiresAt)) {
            throw new IllegalArgumentException("Pickup deadline cannot be after the expiration time");
        }

        FoodListing listing = FoodListing.builder()
                .providerOrganizationId(org.getOrganizationId())
                .foodName(req.getFoodName().trim())
                .description(req.getDescription())
                .category(req.getCategory() != null ? req.getCategory().trim().toUpperCase() : "GENERAL")
                .totalQuantity(req.getTotalQuantity())
                .availableQuantity(req.getTotalQuantity())
                .unit(req.getUnit().trim())
                .dietaryInfo(req.getDietaryInfo())
                .allergenNotes(req.getAllergenNotes())
                .preparedAt(preparedAt)
                .expiresAt(expiresAt)
                .pickupDeadline(pickupDeadline)
                .pickupAddress(req.getPickupAddress() != null && !req.getPickupAddress().isBlank() ? req.getPickupAddress().trim() : org.getAddress())
                .pickupCity(req.getPickupCity() != null && !req.getPickupCity().isBlank() ? req.getPickupCity().trim() : org.getCity())
                .listingStatus("ACTIVE")
                .providerName(org.getOrganizationName())
                .build();

        Long listingId = foodListingRepository.save(listing);
        listing.setListingId(listingId);

        auditService.log(providerUserId, "FOOD_LISTING_CREATED", "FOOD_LISTING",
                String.valueOf(listingId), "Listing created: " + listing.getFoodName() + " (" + listing.getTotalQuantity() + " " + listing.getUnit() + ")");

        return listing;
    }

    public PageResponse<FoodListing> getActive(String category, String city, int page, int size) {
        if (size <= 0) {
            size = 10;
        }
        if (page < 0) {
            page = 0;
        }

        List<FoodListing> content = foodListingRepository.findActive(category, city, page, size);
        long totalElements = foodListingRepository.countByStatus("ACTIVE");
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean last = (page + 1) >= totalPages;

        return PageResponse.<FoodListing>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .last(last)
                .build();
    }

    public PageResponse<FoodListing> getByProvider(Long providerUserId, int page, int size) {
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
        List<FoodListing> content = foodListingRepository.findByProviderOrgId(orgId, page, size);
        long totalElements = foodListingRepository.countByProviderOrgId(orgId);
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean last = (page + 1) >= totalPages;

        return PageResponse.<FoodListing>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .last(last)
                .build();
    }

    public FoodListing getById(Long listingId) {
        if (listingId == null) {
            throw new IllegalArgumentException("Listing ID cannot be null");
        }
        return foodListingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Food listing not found with id: " + listingId));
    }

    @Transactional
    public FoodListing update(Long listingId, FoodListingRequest req, Long userId) {
        if (listingId == null || req == null || userId == null) {
            throw new IllegalArgumentException("Listing ID, request body, and user ID cannot be null");
        }

        FoodListing listing = getById(listingId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole()) || "ROLE_ADMIN".equalsIgnoreCase(user.getRole());
        boolean isOwner = listing.getProviderOrganizationId().equals(user.getOrganizationId());

        if (!isAdmin && !isOwner) {
            throw new UnauthorizedActionException("You are not authorized to update this food listing");
        }

        if (!"ACTIVE".equalsIgnoreCase(listing.getListingStatus())) {
            throw new IllegalStateException("Only ACTIVE listings can be updated. Current status: " + listing.getListingStatus());
        }

        if (req.getFoodName() != null && !req.getFoodName().isBlank()) {
            listing.setFoodName(req.getFoodName().trim());
        }
        if (req.getDescription() != null) {
            listing.setDescription(req.getDescription());
        }
        if (req.getCategory() != null && !req.getCategory().isBlank()) {
            listing.setCategory(req.getCategory().trim().toUpperCase());
        }
        if (req.getTotalQuantity() != null && req.getTotalQuantity().signum() > 0) {
            listing.setTotalQuantity(req.getTotalQuantity());
            if (listing.getAvailableQuantity().compareTo(req.getTotalQuantity()) > 0) {
                listing.setAvailableQuantity(req.getTotalQuantity());
            }
        }
        if (req.getUnit() != null && !req.getUnit().isBlank()) {
            listing.setUnit(req.getUnit().trim());
        }
        if (req.getDietaryInfo() != null) {
            listing.setDietaryInfo(req.getDietaryInfo());
        }
        if (req.getAllergenNotes() != null) {
            listing.setAllergenNotes(req.getAllergenNotes());
        }
        if (req.getPickupAddress() != null && !req.getPickupAddress().isBlank()) {
            listing.setPickupAddress(req.getPickupAddress().trim());
        }
        if (req.getPickupCity() != null && !req.getPickupCity().isBlank()) {
            listing.setPickupCity(req.getPickupCity().trim());
        }

        LocalDateTime now = LocalDateTime.now();
        if (req.getExpiresAt() != null && !req.getExpiresAt().isBlank()) {
            LocalDateTime expiresAt = parseDateTime(req.getExpiresAt());
            if (expiresAt != null && expiresAt.isAfter(now)) {
                listing.setExpiresAt(expiresAt);
            }
        }
        if (req.getPickupDeadline() != null && !req.getPickupDeadline().isBlank()) {
            LocalDateTime deadline = parseDateTime(req.getPickupDeadline());
            if (deadline != null && deadline.isAfter(now)) {
                listing.setPickupDeadline(deadline);
            }
        }
        if (req.getPreparedAt() != null && !req.getPreparedAt().isBlank()) {
            listing.setPreparedAt(parseDateTime(req.getPreparedAt()));
        }

        foodListingRepository.update(listing);

        auditService.log(userId, "FOOD_LISTING_UPDATED", "FOOD_LISTING",
                String.valueOf(listingId), "Listing updated: " + listing.getFoodName());

        return listing;
    }

    @Transactional
    public void cancel(Long listingId, Long userId) {
        if (listingId == null || userId == null) {
            throw new IllegalArgumentException("Listing ID and user ID cannot be null");
        }

        FoodListing listing = getById(listingId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole()) || "ROLE_ADMIN".equalsIgnoreCase(user.getRole());
        boolean isOwner = listing.getProviderOrganizationId().equals(user.getOrganizationId());

        if (!isAdmin && !isOwner) {
            throw new UnauthorizedActionException("You are not authorized to cancel this food listing");
        }

        foodListingRepository.updateStatus(listingId, "CANCELLED");

        auditService.log(userId, "FOOD_LISTING_CANCELLED", "FOOD_LISTING",
                String.valueOf(listingId), "Listing status set to CANCELLED");
    }

    public int expireOverdueListings() {
        return foodListingRepository.expireOverdueListings();
    }

    private LocalDateTime parseDateTime(String str) {
        if (str == null || str.isBlank()) {
            return null;
        }
        str = str.trim();
        try {
            return LocalDateTime.parse(str);
        } catch (DateTimeParseException e) {
            try {
                return java.time.OffsetDateTime.parse(str).toLocalDateTime();
            } catch (DateTimeParseException e2) {
                return java.time.Instant.parse(str).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
            }
        }
    }
}

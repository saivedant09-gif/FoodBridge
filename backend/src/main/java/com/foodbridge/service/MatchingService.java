package com.foodbridge.service;

import com.foodbridge.model.FoodListing;
import com.foodbridge.model.Organization;
import com.foodbridge.repository.FoodListingRepository;
import com.foodbridge.repository.OrganizationRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MatchingService {

    private final FoodListingRepository foodListingRepository;
    private final OrganizationRepository organizationRepository;

    public MatchingService(FoodListingRepository foodListingRepository,
                           OrganizationRepository organizationRepository) {
        this.foodListingRepository = foodListingRepository;
        this.organizationRepository = organizationRepository;
    }

    public List<Map<String, Object>> getRecommendedListings(Long recipientOrgId, int limit) {
        if (limit <= 0) {
            limit = 10;
        }

        Organization recipientOrg = null;
        if (recipientOrgId != null) {
            recipientOrg = organizationRepository.findById(recipientOrgId).orElse(null);
        }

        int verificationScore = 0;
        if (recipientOrg != null && "APPROVED".equalsIgnoreCase(recipientOrg.getVerificationStatus())) {
            verificationScore = 20;
        }

        List<FoodListing> activeListings = foodListingRepository.findActive(null, null, 0, 100);
        LocalDateTime now = LocalDateTime.now();

        List<Map<String, Object>> scoredListings = new ArrayList<>();

        for (FoodListing listing : activeListings) {
            // 1. Category Score (up to 25 points)
            int categoryScore = 15;
            if (listing.getCategory() != null &&
                    ("COOKED_MEAL".equalsIgnoreCase(listing.getCategory()) || "COOKED".equalsIgnoreCase(listing.getCategory()))) {
                categoryScore = 25;
            }

            // 2. Quantity Score (up to 20 points if available_quantity >= threshold)
            int quantityScore = 10;
            if (listing.getAvailableQuantity() != null && listing.getAvailableQuantity().compareTo(new BigDecimal("5.0")) >= 0) {
                quantityScore = 20;
            }

            // 3. Urgency Score (up to 15 points, sooner deadline = higher score)
            int urgencyScore = 5;
            if (listing.getPickupDeadline() != null) {
                long hoursUntilDeadline = Duration.between(now, listing.getPickupDeadline()).toHours();
                if (hoursUntilDeadline <= 6) {
                    urgencyScore = 15;
                } else if (hoursUntilDeadline <= 12) {
                    urgencyScore = 12;
                } else if (hoursUntilDeadline <= 24) {
                    urgencyScore = 10;
                } else if (hoursUntilDeadline <= 48) {
                    urgencyScore = 7;
                } else {
                    urgencyScore = 4;
                }
            }

            // 4. Verification Score (20 points if APPROVED)
            // calculated above

            // 5. Collection Feasibility Score (20 points)
            int feasibilityScore = 20;

            int totalScore = categoryScore + quantityScore + urgencyScore + verificationScore + feasibilityScore;
            totalScore = Math.min(100, Math.max(0, totalScore));

            String explanation = String.format("Score breakdown: Category(%d) + Quantity(%d) + Urgency(%d) + Verification(%d) + Feasibility(%d)",
                    categoryScore, quantityScore, urgencyScore, verificationScore, feasibilityScore);

            Map<String, Object> item = new HashMap<>();
            item.put("listing", listing);
            item.put("listingId", listing.getListingId());
            item.put("foodName", listing.getFoodName());
            item.put("category", listing.getCategory());
            item.put("availableQuantity", listing.getAvailableQuantity());
            item.put("unit", listing.getUnit());
            item.put("pickupDeadline", listing.getPickupDeadline());
            item.put("pickupAddress", listing.getPickupAddress());
            item.put("pickupCity", listing.getPickupCity());
            item.put("providerName", listing.getProviderName());
            item.put("matchScore", totalScore);
            item.put("explanation", explanation);

            scoredListings.add(item);
        }

        return scoredListings.stream()
                .sorted((a, b) -> Integer.compare((int) b.get("matchScore"), (int) a.get("matchScore")))
                .limit(limit)
                .collect(Collectors.toList());
    }
}

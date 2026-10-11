package com.foodbridge;

import com.foodbridge.model.FoodListing;
import com.foodbridge.model.Organization;
import com.foodbridge.repository.FoodListingRepository;
import com.foodbridge.repository.OrganizationRepository;
import com.foodbridge.service.MatchingService;
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
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MatchingService verifying:
 * - Rule-based scoring engine (category, quantity, urgency, verification, feasibility)
 * - Sorting recommendations by score descending
 * - Score explanation transparency
 * - Graceful handling of empty listings
 */
@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock private FoodListingRepository foodListingRepository;
    @Mock private OrganizationRepository organizationRepository;

    private MatchingService matchingService;

    @BeforeEach
    void setUp() {
        matchingService = new MatchingService(foodListingRepository, organizationRepository);
    }

    @Test
    @DisplayName("Should rank listings by calculated score and include score explanations")
    void getRecommendedListings_returnsRanked() {
        Long recipientOrgId = 5L;
        Organization verifiedRecipient = Organization.builder()
                .organizationId(recipientOrgId)
                .verificationStatus("APPROVED")
                .build();

        // Listing 1: Cooked meal, high urgency (2 hours left) -> highest score
        FoodListing urgentCookedMeal = FoodListing.builder()
                .listingId(1L)
                .foodName("Hot Biryani")
                .category("COOKED_MEAL")
                .availableQuantity(new BigDecimal("30.00"))
                .pickupDeadline(LocalDateTime.now().plusHours(2))
                .build();

        // Listing 2: Packaged snacks, low urgency (24 hours left)
        FoodListing packagedSnack = FoodListing.builder()
                .listingId(2L)
                .foodName("Biscuits")
                .category("SNACKS")
                .availableQuantity(new BigDecimal("10.00"))
                .pickupDeadline(LocalDateTime.now().plusHours(24))
                .build();

        when(organizationRepository.findById(recipientOrgId)).thenReturn(Optional.of(verifiedRecipient));
        when(foodListingRepository.findActive(null, null, 0, 100))
                .thenReturn(List.of(packagedSnack, urgentCookedMeal));

        List<Map<String, Object>> recommendations = matchingService.getRecommendedListings(recipientOrgId, 10);

        assertNotNull(recommendations);
        assertEquals(2, recommendations.size());

        // First item should be the higher scored one (Biryani)
        Map<String, Object> first = recommendations.get(0);
        FoodListing firstListing = (FoodListing) first.get("listing");
        assertEquals("Hot Biryani", firstListing.getFoodName());

        int score1 = (int) first.get("matchScore");
        int score2 = (int) recommendations.get(1).get("matchScore");
        assertTrue(score1 >= score2, "Results must be ordered by match score descending");

        // Explanation must be present
        String explanation = (String) first.get("explanation");
        assertNotNull(explanation);
        assertTrue(explanation.contains("Score breakdown"));
    }

    @Test
    @DisplayName("Should return empty list when no active food listings exist")
    void getRecommendedListings_emptyListings() {
        when(organizationRepository.findById(anyLong())).thenReturn(Optional.empty());
        when(foodListingRepository.findActive(null, null, 0, 100)).thenReturn(Collections.emptyList());

        List<Map<String, Object>> recommendations = matchingService.getRecommendedListings(1L, 10);

        assertNotNull(recommendations);
        assertTrue(recommendations.isEmpty());
    }
}

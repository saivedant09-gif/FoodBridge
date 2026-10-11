package com.foodbridge;

import com.foodbridge.dto.ImpactSummaryResponse;
import com.foodbridge.exception.ResourceNotFoundException;
import com.foodbridge.model.User;
import com.foodbridge.repository.AnalyticsRepository;
import com.foodbridge.repository.UserRepository;
import com.foodbridge.service.AnalyticsService;
import com.foodbridge.util.ImpactCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AnalyticsService & ImpactCalculator verifying:
 * - Aggregated platform metrics retrieval
 * - Accurate fulfillment rate math (preventing divide-by-zero)
 * - Category and trend chart data delegation
 * - Provider and recipient statistics lookup
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock private AnalyticsRepository analyticsRepository;
    @Mock private UserRepository userRepository;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(analyticsRepository, userRepository);
    }

    @Test
    @DisplayName("Should return platform impact summary from repository")
    void getPlatformSummary_correctValues() {
        ImpactSummaryResponse expected = ImpactSummaryResponse.builder()
                .verifiedOrgs(6L)
                .totalListings(8L)
                .activeListings(4L)
                .completedListings(1L)
                .expiredListings(1L)
                .totalRequests(8L)
                .fulfilledRequests(3L)
                .totalAllocated(new BigDecimal("270.00"))
                .completedCollections(3L)
                .providerUsers(4L)
                .recipientUsers(4L)
                .fulfillmentRate(37.5)
                .build();

        when(analyticsRepository.getPlatformSummary()).thenReturn(expected);

        ImpactSummaryResponse summary = analyticsService.getPlatformSummary();

        assertNotNull(summary);
        assertEquals(6L, summary.getVerifiedOrgs());
        assertEquals(8L, summary.getTotalListings());
        assertEquals(37.5, summary.getFulfillmentRate());
        assertEquals(new BigDecimal("270.00"), summary.getTotalAllocated());
        verify(analyticsRepository, times(1)).getPlatformSummary();
    }

    @Test
    @DisplayName("ImpactCalculator fulfillment rate should compute correctly and guard against zero division")
    void fulfillmentRate_calculation() {
        // 3 fulfilled out of 5 total -> 60.0%
        double rate1 = ImpactCalculator.fulfillmentRate(3, 5);
        assertEquals(60.0, rate1, 0.001);

        // 0 fulfilled out of 10 total -> 0.0%
        double rate2 = ImpactCalculator.fulfillmentRate(0, 10);
        assertEquals(0.0, rate2, 0.001);

        // 0 total requests -> 0.0% without division by zero exception
        double rate3 = ImpactCalculator.fulfillmentRate(0, 0);
        assertEquals(0.0, rate3, 0.001);
    }

    @Test
    @DisplayName("Should return category distribution data for chart visualization")
    void getCategoryDistribution() {
        List<Map<String, Object>> mockData = List.of(
                Map.of("category", "COOKED_MEAL", "listing_count", 4, "total_qty", 290.0),
                Map.of("category", "BAKERY", "listing_count", 3, "total_qty", 80.0)
        );

        when(analyticsRepository.getCategoryDistribution()).thenReturn(mockData);

        List<Map<String, Object>> result = analyticsService.getCategoryDistribution();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("COOKED_MEAL", result.get(0).get("category"));
    }

    @Test
    @DisplayName("Should retrieve provider stats for associated user organization")
    void getProviderStats_success() {
        Long userId = 10L;
        Long orgId = 1L;

        User user = User.builder().userId(userId).organizationId(orgId).role("PROVIDER").build();
        Map<String, Object> stats = Map.of("total_listings", 3, "active_listings", 2);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(analyticsRepository.getProviderStats(orgId)).thenReturn(stats);

        Map<String, Object> result = analyticsService.getProviderStats(userId);

        assertNotNull(result);
        assertEquals(3, result.get("total_listings"));
        verify(analyticsRepository, times(1)).getProviderStats(orgId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user has no organization")
    void getProviderStats_userWithoutOrg() {
        Long userId = 10L;
        User user = User.builder().userId(userId).organizationId(null).role("PROVIDER").build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(ResourceNotFoundException.class, () -> analyticsService.getProviderStats(userId));
        verify(analyticsRepository, never()).getProviderStats(any());
    }
}

package com.foodbridge.controller;

import com.foodbridge.dto.ApiResponse;
import com.foodbridge.dto.ImpactSummaryResponse;
import com.foodbridge.model.User;
import com.foodbridge.repository.UserRepository;
import com.foodbridge.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final UserRepository userRepository;

    public AnalyticsController(AnalyticsService analyticsService, UserRepository userRepository) {
        this.analyticsService = analyticsService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + auth.getName()));
    }

    @GetMapping("/analytics/provider")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProviderAnalytics(Authentication authentication) {
        User user = getCurrentUser(authentication);
        Map<String, Object> stats = analyticsService.getProviderStats(user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Provider analytics retrieved", stats));
    }

    @GetMapping("/analytics/recipient")
    @PreAuthorize("hasRole('RECIPIENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRecipientAnalytics(Authentication authentication) {
        User user = getCurrentUser(authentication);
        Map<String, Object> stats = analyticsService.getRecipientStats(user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Recipient analytics retrieved", stats));
    }

    @GetMapping("/reports/impact")
    public ResponseEntity<ApiResponse<ImpactSummaryResponse>> getImpactReport() {
        ImpactSummaryResponse summary = analyticsService.getPlatformSummary();
        return ResponseEntity.ok(ApiResponse.ok("Platform impact summary retrieved", summary));
    }

    @GetMapping("/analytics/categories")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCategoryDistribution() {
        List<Map<String, Object>> distribution = analyticsService.getCategoryDistribution();
        return ResponseEntity.ok(ApiResponse.ok("Category distribution retrieved", distribution));
    }

    @GetMapping("/analytics/monthly-collections")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMonthlyCollections() {
        List<Map<String, Object>> trends = analyticsService.getMonthlyCollections();
        return ResponseEntity.ok(ApiResponse.ok("Monthly collection trends retrieved", trends));
    }

    @GetMapping("/analytics/request-status")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRequestStatusDistribution() {
        List<Map<String, Object>> distribution = analyticsService.getRequestStatusDistribution();
        return ResponseEntity.ok(ApiResponse.ok("Request status distribution retrieved", distribution));
    }
}

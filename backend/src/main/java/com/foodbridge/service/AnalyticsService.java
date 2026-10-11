package com.foodbridge.service;

import com.foodbridge.dto.ImpactSummaryResponse;
import com.foodbridge.exception.ResourceNotFoundException;
import com.foodbridge.model.User;
import com.foodbridge.repository.AnalyticsRepository;
import com.foodbridge.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;
    private final UserRepository userRepository;

    public AnalyticsService(AnalyticsRepository analyticsRepository,
                            UserRepository userRepository) {
        this.analyticsRepository = analyticsRepository;
        this.userRepository = userRepository;
    }

    public ImpactSummaryResponse getPlatformSummary() {
        return analyticsRepository.getPlatformSummary();
    }

    public List<Map<String, Object>> getCategoryDistribution() {
        return analyticsRepository.getCategoryDistribution();
    }

    public List<Map<String, Object>> getMonthlyCollections() {
        return analyticsRepository.getMonthlyCollections();
    }

    public List<Map<String, Object>> getRequestStatusDistribution() {
        return analyticsRepository.getRequestStatusDistribution();
    }

    public Map<String, Object> getProviderStats(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getOrganizationId() == null) {
            throw new ResourceNotFoundException("User is not associated with an organization");
        }

        return analyticsRepository.getProviderStats(user.getOrganizationId());
    }

    public Map<String, Object> getRecipientStats(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getOrganizationId() == null) {
            throw new ResourceNotFoundException("User is not associated with an organization");
        }

        return analyticsRepository.getRecipientStats(user.getOrganizationId());
    }
}

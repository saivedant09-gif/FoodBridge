package com.foodbridge.controller;

import com.foodbridge.dto.ApiResponse;
import com.foodbridge.dto.ImpactSummaryResponse;
import com.foodbridge.dto.PageResponse;
import com.foodbridge.model.AuditLog;
import com.foodbridge.model.Issue;
import com.foodbridge.model.Organization;
import com.foodbridge.model.User;
import com.foodbridge.repository.IssueRepository;
import com.foodbridge.repository.UserRepository;
import com.foodbridge.service.AnalyticsService;
import com.foodbridge.service.AuditService;
import com.foodbridge.service.OrganizationService;
import com.foodbridge.service.UserService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AnalyticsService analyticsService;
    private final UserService userService;
    private final OrganizationService organizationService;
    private final AuditService auditService;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;

    public AdminController(AnalyticsService analyticsService,
                           UserService userService,
                           OrganizationService organizationService,
                           AuditService auditService,
                           IssueRepository issueRepository,
                           UserRepository userRepository) {
        this.analyticsService = analyticsService;
        this.userService = userService;
        this.organizationService = organizationService;
        this.auditService = auditService;
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
    }

    private User getCurrentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + auth.getName()));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusDto {
        private String status;
    }

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<ImpactSummaryResponse>> getAdminAnalytics() {
        ImpactSummaryResponse summary = analyticsService.getPlatformSummary();
        return ResponseEntity.ok(ApiResponse.ok("Admin platform summary retrieved", summary));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PageResponse<User>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<User> users = userService.getAllUsers(page, size);
        return ResponseEntity.ok(ApiResponse.ok("Users retrieved successfully", users));
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<String>> updateUserStatus(
            @PathVariable("id") Long id,
            @RequestBody StatusDto body,
            Authentication authentication) {
        User admin = getCurrentUser(authentication);
        userService.updateUserStatus(id, body.getStatus(), admin.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("User status updated successfully", "User status updated"));
    }

    @GetMapping("/organizations/pending")
    public ResponseEntity<ApiResponse<PageResponse<Organization>>> getPendingOrganizations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<Organization> orgs = organizationService.getPendingVerifications(page, size);
        return ResponseEntity.ok(ApiResponse.ok("Pending organizations retrieved successfully", orgs));
    }

    @PatchMapping("/organizations/{id}/verification")
    public ResponseEntity<ApiResponse<String>> verifyOrganization(
            @PathVariable("id") Long id,
            @RequestBody StatusDto body,
            Authentication authentication) {
        User admin = getCurrentUser(authentication);
        organizationService.verifyOrganization(id, body.getStatus(), admin.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Organization verification updated successfully", "Verification updated"));
    }

    @GetMapping("/audit")
    public ResponseEntity<ApiResponse<PageResponse<AuditLog>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<AuditLog> auditLogs = auditService.getAll(page, size);
        return ResponseEntity.ok(ApiResponse.ok("Audit logs retrieved successfully", auditLogs));
    }

    @GetMapping("/issues")
    public ResponseEntity<ApiResponse<List<Issue>>> getIssues() {
        List<Issue> issues = issueRepository.findAll();
        return ResponseEntity.ok(ApiResponse.ok("Issues retrieved successfully", issues));
    }
}

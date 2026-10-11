package com.foodbridge.controller;

import com.foodbridge.dto.ApiResponse;
import com.foodbridge.model.Organization;
import com.foodbridge.model.User;
import com.foodbridge.repository.UserRepository;
import com.foodbridge.service.OrganizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final UserRepository userRepository;

    public OrganizationController(OrganizationService organizationService, UserRepository userRepository) {
        this.organizationService = organizationService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + auth.getName()));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Organization>> getMyOrganization(Authentication authentication) {
        User user = getCurrentUser(authentication);
        Organization org = organizationService.getByUserId(user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Organization details retrieved", org));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<Organization>> updateMyOrganization(@RequestBody Organization updateData,
                                                                           Authentication authentication) {
        User user = getCurrentUser(authentication);
        Organization existingOrg = organizationService.getByUserId(user.getUserId());
        Organization updated = organizationService.update(existingOrg.getOrganizationId(), user.getUserId(), updateData);
        return ResponseEntity.ok(ApiResponse.ok("Organization updated successfully", updated));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Organization>> getOrganizationById(@PathVariable("id") Long id) {
        Organization org = organizationService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok("Organization details retrieved", org));
    }
}

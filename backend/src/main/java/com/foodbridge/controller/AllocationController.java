package com.foodbridge.controller;

import com.foodbridge.dto.ApiResponse;
import com.foodbridge.dto.PageResponse;
import com.foodbridge.model.Allocation;
import com.foodbridge.model.User;
import com.foodbridge.repository.UserRepository;
import com.foodbridge.service.AllocationService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class AllocationController {

    private final AllocationService allocationService;
    private final UserRepository userRepository;

    public AllocationController(AllocationService allocationService, UserRepository userRepository) {
        this.allocationService = allocationService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + auth.getName()));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelAllocationDto {
        private String reason;
    }

    @PostMapping("/requests/{requestId}/approve")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<ApiResponse<Allocation>> approveRequest(
            @PathVariable("requestId") Long requestId,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        Allocation allocation = allocationService.approve(requestId, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Request approved and allocation created successfully", allocation));
    }

    @GetMapping("/allocations/me")
    @PreAuthorize("hasRole('RECIPIENT')")
    public ResponseEntity<ApiResponse<PageResponse<Allocation>>> getMyAllocations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        PageResponse<Allocation> allocations = allocationService.getMyAllocations(user.getUserId(), page, size);
        return ResponseEntity.ok(ApiResponse.ok("Allocations retrieved successfully", allocations));
    }

    @GetMapping("/allocations/{id}")
    public ResponseEntity<ApiResponse<Allocation>> getAllocationById(
            @PathVariable("id") Long id,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        Allocation allocation = allocationService.getById(id, user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Allocation details retrieved", allocation));
    }

    @PostMapping("/allocations/{id}/cancel")
    public ResponseEntity<ApiResponse<String>> cancelAllocation(
            @PathVariable("id") Long id,
            @RequestBody(required = false) CancelAllocationDto body,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        String reason = (body != null) ? body.getReason() : null;
        allocationService.cancel(id, reason, user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Allocation cancelled successfully", "Allocation cancelled"));
    }
}

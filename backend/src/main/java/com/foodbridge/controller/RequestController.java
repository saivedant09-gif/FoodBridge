package com.foodbridge.controller;

import com.foodbridge.dto.ApiResponse;
import com.foodbridge.dto.PageResponse;
import com.foodbridge.model.FoodRequest;
import com.foodbridge.model.User;
import com.foodbridge.repository.UserRepository;
import com.foodbridge.service.RequestService;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1")
public class RequestController {

    private final RequestService requestService;
    private final UserRepository userRepository;

    public RequestController(RequestService requestService, UserRepository userRepository) {
        this.requestService = requestService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + auth.getName()));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequestDto {
        @NotNull(message = "Requested quantity is required")
        @DecimalMin(value = "0.01", message = "Requested quantity must be greater than 0")
        private BigDecimal requestedQuantity;
        private String message;
    }

    @PostMapping("/listings/{listingId}/requests")
    @PreAuthorize("hasRole('RECIPIENT')")
    public ResponseEntity<ApiResponse<FoodRequest>> submitRequest(
            @PathVariable("listingId") Long listingId,
            @RequestBody CreateRequestDto body,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        FoodRequest foodRequest = requestService.submit(
                listingId,
                body.getRequestedQuantity(),
                body.getMessage(),
                user.getUserId()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Food request submitted successfully", foodRequest));
    }

    @GetMapping("/requests/me")
    @PreAuthorize("hasRole('RECIPIENT')")
    public ResponseEntity<ApiResponse<PageResponse<FoodRequest>>> getMyRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        PageResponse<FoodRequest> requests = requestService.getMyRequests(user.getUserId(), page, size);
        return ResponseEntity.ok(ApiResponse.ok("Requests retrieved successfully", requests));
    }

    @GetMapping("/provider/requests")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<ApiResponse<PageResponse<FoodRequest>>> getProviderRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        PageResponse<FoodRequest> requests = requestService.getPendingForProvider(user.getUserId(), page, size);
        return ResponseEntity.ok(ApiResponse.ok("Pending requests retrieved successfully", requests));
    }

    @GetMapping("/requests/{id}")
    public ResponseEntity<ApiResponse<FoodRequest>> getRequestById(
            @PathVariable("id") Long id,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        FoodRequest foodRequest = requestService.getById(id, user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Request details retrieved", foodRequest));
    }

    @PatchMapping("/requests/{id}/cancel")
    @PreAuthorize("hasRole('RECIPIENT')")
    public ResponseEntity<ApiResponse<String>> cancelRequest(
            @PathVariable("id") Long id,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        requestService.cancel(id, user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Request cancelled successfully", "Request cancelled"));
    }
}

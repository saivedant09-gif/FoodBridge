package com.foodbridge.controller;

import com.foodbridge.dto.ApiResponse;
import com.foodbridge.dto.FoodListingRequest;
import com.foodbridge.dto.PageResponse;
import com.foodbridge.model.FoodListing;
import com.foodbridge.model.User;
import com.foodbridge.repository.UserRepository;
import com.foodbridge.service.FoodListingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class FoodListingController {

    private final FoodListingService foodListingService;
    private final UserRepository userRepository;

    public FoodListingController(FoodListingService foodListingService, UserRepository userRepository) {
        this.foodListingService = foodListingService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + auth.getName()));
    }

    @GetMapping("/listings")
    public ResponseEntity<ApiResponse<PageResponse<FoodListing>>> getListings(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<FoodListing> listings = foodListingService.getActive(category, city, page, size);
        return ResponseEntity.ok(ApiResponse.ok("Listings retrieved successfully", listings));
    }

    @GetMapping("/listings/{id}")
    public ResponseEntity<ApiResponse<FoodListing>> getListingById(@PathVariable("id") Long id) {
        FoodListing listing = foodListingService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok("Listing details retrieved", listing));
    }

    @PostMapping("/listings")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<ApiResponse<FoodListing>> createListing(
            @Valid @RequestBody FoodListingRequest request,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        FoodListing created = foodListingService.create(request, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Food listing created successfully", created));
    }

    @PutMapping("/listings/{id}")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<ApiResponse<FoodListing>> updateListing(
            @PathVariable("id") Long id,
            @Valid @RequestBody FoodListingRequest request,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        FoodListing updated = foodListingService.update(id, request, user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Food listing updated successfully", updated));
    }

    @DeleteMapping("/listings/{id}")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<ApiResponse<String>> cancelListing(
            @PathVariable("id") Long id,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        foodListingService.cancel(id, user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Food listing cancelled successfully", "Listing cancelled"));
    }

    @GetMapping("/provider/listings")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<ApiResponse<PageResponse<FoodListing>>> getMyListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        PageResponse<FoodListing> listings = foodListingService.getByProvider(user.getUserId(), page, size);
        return ResponseEntity.ok(ApiResponse.ok("Provider listings retrieved successfully", listings));
    }
}

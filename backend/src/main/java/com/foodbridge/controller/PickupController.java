package com.foodbridge.controller;

import com.foodbridge.dto.ApiResponse;
import com.foodbridge.dto.PageResponse;
import com.foodbridge.model.CollectionRecord;
import com.foodbridge.model.User;
import com.foodbridge.repository.UserRepository;
import com.foodbridge.service.PickupService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class PickupController {

    private final PickupService pickupService;
    private final UserRepository userRepository;

    public PickupController(PickupService pickupService, UserRepository userRepository) {
        this.pickupService = pickupService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + auth.getName()));
    }

    @GetMapping("/collections/me")
    public ResponseEntity<ApiResponse<PageResponse<CollectionRecord>>> getMyCollections(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        PageResponse<CollectionRecord> records = pickupService.getMyCollections(user.getUserId(), user.getRole(), page, size);
        return ResponseEntity.ok(ApiResponse.ok("Collections retrieved successfully", records));
    }

    @GetMapping("/collections/{id}")
    public ResponseEntity<ApiResponse<CollectionRecord>> getCollectionById(
            @PathVariable("id") Long id,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        CollectionRecord record = pickupService.getById(id, user.getUserId(), user.getRole());
        return ResponseEntity.ok(ApiResponse.ok("Collection record retrieved", record));
    }

    @PostMapping("/collections/{id}/confirm-provider")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<ApiResponse<CollectionRecord>> confirmProvider(
            @PathVariable("id") Long id,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        CollectionRecord record = pickupService.confirmProvider(id, user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Handover confirmed by provider", record));
    }

    @PostMapping("/collections/{id}/confirm-recipient")
    @PreAuthorize("hasRole('RECIPIENT')")
    public ResponseEntity<ApiResponse<CollectionRecord>> confirmRecipient(
            @PathVariable("id") Long id,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        CollectionRecord record = pickupService.confirmRecipient(id, user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok("Receipt confirmed by recipient", record));
    }
}

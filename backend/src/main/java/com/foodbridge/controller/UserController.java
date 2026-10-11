package com.foodbridge.controller;

import com.foodbridge.dto.ApiResponse;
import com.foodbridge.model.User;
import com.foodbridge.repository.UserRepository;
import com.foodbridge.service.UserService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + auth.getName()));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateUserDto {
        private String fullName;
        private String phone;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> getMe(Authentication authentication) {
        User user = getCurrentUser(authentication);
        user.setPasswordHash(null);
        return ResponseEntity.ok(ApiResponse.ok("User profile retrieved successfully", user));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<User>> updateMe(
            @RequestBody UpdateUserDto request,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        User updated = userService.updateProfile(user.getUserId(), request.getFullName(), request.getPhone());
        updated.setPasswordHash(null);
        return ResponseEntity.ok(ApiResponse.ok("User profile updated successfully", updated));
    }
}

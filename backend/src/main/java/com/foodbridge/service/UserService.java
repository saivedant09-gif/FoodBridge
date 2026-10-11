package com.foodbridge.service;

import com.foodbridge.dto.PageResponse;
import com.foodbridge.exception.ResourceNotFoundException;
import com.foodbridge.model.User;
import com.foodbridge.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuditService auditService;

    public UserService(UserRepository userRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    public User getById(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    public User getByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be blank");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Transactional
    public User updateProfile(Long userId, String fullName, String phone) {
        User user = getById(userId);

        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName.trim());
        }
        if (phone != null) {
            user.setPhone(phone.trim());
        }

        userRepository.update(user);

        auditService.log(userId, "USER_PROFILE_UPDATED", "USER",
                String.valueOf(userId), "User profile updated");

        return user;
    }

    @Transactional
    public void updateUserStatus(Long userId, String status, Long adminUserId) {
        if (userId == null || status == null || status.isBlank()) {
            throw new IllegalArgumentException("User ID and status must not be null or blank");
        }
        String normalizedStatus = status.trim().toUpperCase();
        userRepository.updateStatus(userId, normalizedStatus);

        auditService.log(adminUserId, "USER_STATUS_UPDATED", "USER",
                String.valueOf(userId), "Status updated to " + normalizedStatus);
    }

    public PageResponse<User> getAllUsers(int page, int size) {
        if (size <= 0) {
            size = 10;
        }
        if (page < 0) {
            page = 0;
        }

        List<User> content = userRepository.findAll(page, size);
        long totalElements = userRepository.count();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean last = (page + 1) >= totalPages;

        return PageResponse.<User>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .last(last)
                .build();
    }
}

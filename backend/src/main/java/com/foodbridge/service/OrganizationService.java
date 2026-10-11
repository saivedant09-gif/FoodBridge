package com.foodbridge.service;

import com.foodbridge.dto.PageResponse;
import com.foodbridge.exception.ResourceNotFoundException;
import com.foodbridge.exception.UnauthorizedActionException;
import com.foodbridge.model.Organization;
import com.foodbridge.model.User;
import com.foodbridge.repository.OrganizationRepository;
import com.foodbridge.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public OrganizationService(OrganizationRepository organizationRepository,
                               UserRepository userRepository,
                               AuditService auditService,
                               NotificationService notificationService) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    public Organization getByUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        return organizationRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found for user id: " + userId));
    }

    public Organization getById(Long orgId) {
        if (orgId == null) {
            throw new IllegalArgumentException("Organization ID cannot be null");
        }
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + orgId));
    }

    @Transactional
    public Organization update(Long orgId, Long requestingUserId, Organization updateData) {
        if (orgId == null || requestingUserId == null || updateData == null) {
            throw new IllegalArgumentException("Organization ID, user ID, and update data must not be null");
        }

        Organization existing = getById(orgId);
        User user = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + requestingUserId));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(user.getRole()) || "ROLE_ADMIN".equalsIgnoreCase(user.getRole());
        boolean isOwner = orgId.equals(user.getOrganizationId());

        if (!isAdmin && !isOwner) {
            throw new UnauthorizedActionException("You are not authorized to update this organization");
        }

        if (updateData.getOrganizationName() != null && !updateData.getOrganizationName().isBlank()) {
            existing.setOrganizationName(updateData.getOrganizationName().trim());
        }
        if (updateData.getOrganizationType() != null && !updateData.getOrganizationType().isBlank()) {
            existing.setOrganizationType(updateData.getOrganizationType().trim());
        }
        if (updateData.getDescription() != null) {
            existing.setDescription(updateData.getDescription());
        }
        if (updateData.getContactPhone() != null && !updateData.getContactPhone().isBlank()) {
            existing.setContactPhone(updateData.getContactPhone().trim());
        }
        if (updateData.getContactEmail() != null && !updateData.getContactEmail().isBlank()) {
            existing.setContactEmail(updateData.getContactEmail().trim());
        }
        if (updateData.getAddress() != null && !updateData.getAddress().isBlank()) {
            existing.setAddress(updateData.getAddress().trim());
        }
        if (updateData.getCity() != null && !updateData.getCity().isBlank()) {
            existing.setCity(updateData.getCity().trim());
        }
        if (updateData.getState() != null && !updateData.getState().isBlank()) {
            existing.setState(updateData.getState().trim());
        }
        if (updateData.getPincode() != null && !updateData.getPincode().isBlank()) {
            existing.setPincode(updateData.getPincode().trim());
        }
        if (updateData.getLatitude() != null) {
            existing.setLatitude(updateData.getLatitude());
        }
        if (updateData.getLongitude() != null) {
            existing.setLongitude(updateData.getLongitude());
        }

        organizationRepository.update(existing);

        auditService.log(requestingUserId, "ORGANIZATION_UPDATED", "ORGANIZATION",
                String.valueOf(orgId), "Organization details updated");

        return existing;
    }

    @Transactional
    public void verifyOrganization(Long orgId, String status, Long adminUserId) {
        if (orgId == null || status == null || adminUserId == null) {
            throw new IllegalArgumentException("Organization ID, verification status, and admin user ID must not be null");
        }

        String normalizedStatus = status.trim().toUpperCase();
        if (!"APPROVED".equals(normalizedStatus) && !"REJECTED".equals(normalizedStatus)) {
            throw new IllegalArgumentException("Verification status must be either APPROVED or REJECTED");
        }

        Organization org = getById(orgId);
        organizationRepository.updateVerificationStatus(orgId, normalizedStatus, adminUserId);

        auditService.log(adminUserId, "ORGANIZATION_VERIFIED", "ORGANIZATION",
                String.valueOf(orgId), "Verification status set to " + normalizedStatus);

        List<User> members = userRepository.findByOrganizationId(orgId);
        for (User member : members) {
            notificationService.notifyUser(
                    member.getUserId(),
                    "Organization Verification " + normalizedStatus,
                    "Your organization (" + org.getOrganizationName() + ") has been " + normalizedStatus.toLowerCase() + " by an administrator.",
                    normalizedStatus.equals("APPROVED") ? "SUCCESS" : "WARNING",
                    "ORGANIZATION",
                    orgId
            );
        }
    }

    public PageResponse<Organization> getPendingVerifications(int page, int size) {
        if (size <= 0) {
            size = 10;
        }
        if (page < 0) {
            page = 0;
        }

        List<Organization> content = organizationRepository.findByStatus("PENDING", page, size);
        long totalElements = organizationRepository.countByStatus("PENDING");
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean last = (page + 1) >= totalPages;

        return PageResponse.<Organization>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .last(last)
                .build();
    }
}

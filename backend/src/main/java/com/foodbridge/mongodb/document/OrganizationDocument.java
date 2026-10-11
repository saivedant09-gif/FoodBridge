package com.foodbridge.mongodb.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Flexible supplementary profile data for organizations.
 * Core organization data is in MySQL organizations table.
 */
@Document(collection = "organization_profiles")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrganizationDocument {
    @Id
    private String id;

    @Indexed(unique = true)
    private Long mysqlOrgId;  // FK reference to organizations.organization_id

    private Map<String, String> socialLinks;     // e.g., {"website": "...", "facebook": "..."}
    private String mission;
    private List<String> servicesOffered;
    private String operatingHours;
    private Integer beneficiariesServedMonthly;
    private List<String> certifications;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

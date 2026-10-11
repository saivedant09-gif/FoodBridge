package com.foodbridge.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Issue {
    private Long issueId;
    private Long reportedBy;
    private Long relatedListingId;
    private Long relatedAllocationId;
    private String issueType;
    private String description;
    private String issueStatus;
    private String resolutionNotes;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}

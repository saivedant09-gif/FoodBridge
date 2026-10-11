package com.foodbridge.mongodb.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Optional supplementary metadata for food listings.
 * The canonical listing data is in MySQL food_listings.
 * This document stores flexible additional attributes.
 */
@Document(collection = "listing_metadata")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ListingMetadata {
    @Id
    private String id;

    @Indexed
    private Long mysqlListingId;  // FK reference to food_listings.listing_id

    private List<String> imagePaths;            // optional image references
    private List<String> handlingInstructions;  // e.g., "Keep refrigerated"
    private Map<String, String> additionalAttributes; // flexible key-value
    private String storageInstructions;
    private String packagingNotes;
    private Boolean requiresRefrigeration;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

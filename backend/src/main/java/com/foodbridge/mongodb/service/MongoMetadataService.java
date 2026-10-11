package com.foodbridge.mongodb.service;

import com.foodbridge.mongodb.document.ListingMetadata;
import com.foodbridge.mongodb.repository.ListingMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Facade for all optional MongoDB interactions related to listing metadata.
 *
 * <p>MongoDB is treated as a supplementary store: if it is disabled via
 * {@code foodbridge.mongodb.enabled=false} (the default), or if any call
 * fails at runtime, the method returns an empty/null result and logs a
 * warning so that the core MySQL-backed workflow is never interrupted.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MongoMetadataService {

    private final ListingMetadataRepository listingMetadataRepository;

    /**
     * Feature flag injected from application properties.
     * Defaults to {@code false} so that MongoDB is opt-in.
     */
    @Value("${foodbridge.mongodb.enabled:false}")
    private boolean mongoEnabled;

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /**
     * Retrieve supplementary metadata for a MySQL listing by its primary key.
     *
     * @param listingId the {@code listing_id} from the MySQL {@code food_listings} table
     * @return the document wrapped in an Optional, or {@link Optional#empty()} when
     *         MongoDB is disabled or the document does not exist or an error occurs
     */
    public Optional<ListingMetadata> getByListingId(Long listingId) {
        if (!mongoEnabled) {
            return Optional.empty();
        }
        try {
            return listingMetadataRepository.findByMysqlListingId(listingId);
        } catch (Exception ex) {
            log.warn("[MongoDB] getByListingId failed for listingId={}: {}", listingId, ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Insert or update (upsert) supplementary metadata for a listing.
     *
     * <p>If a document with the same {@code mysqlListingId} already exists its
     * {@code id} is re-used so that Spring Data performs an update rather than
     * creating a duplicate document.
     *
     * @param metadata the document to persist; must have {@code mysqlListingId} set
     * @return the saved document, or {@code null} when MongoDB is disabled or the
     *         operation fails
     */
    public ListingMetadata saveOrUpdate(ListingMetadata metadata) {
        if (!mongoEnabled) {
            return null;
        }
        try {
            // Resolve existing document id for a true upsert via save()
            if (metadata.getMysqlListingId() != null && metadata.getId() == null) {
                listingMetadataRepository
                        .findByMysqlListingId(metadata.getMysqlListingId())
                        .ifPresent(existing -> metadata.setId(existing.getId()));
            }

            LocalDateTime now = LocalDateTime.now();
            if (metadata.getCreatedAt() == null) {
                metadata.setCreatedAt(now);
            }
            metadata.setUpdatedAt(now);

            return listingMetadataRepository.save(metadata);
        } catch (Exception ex) {
            log.warn("[MongoDB] saveOrUpdate failed for listingId={}: {}",
                    metadata.getMysqlListingId(), ex.getMessage());
            return null;
        }
    }

    /**
     * Delete the supplementary metadata document associated with a MySQL listing.
     *
     * @param listingId the {@code listing_id} from the MySQL {@code food_listings} table
     */
    public void deleteByListingId(Long listingId) {
        if (!mongoEnabled) {
            return;
        }
        try {
            listingMetadataRepository.deleteByMysqlListingId(listingId);
        } catch (Exception ex) {
            log.warn("[MongoDB] deleteByListingId failed for listingId={}: {}", listingId, ex.getMessage());
        }
    }

    /**
     * Perform a lightweight connectivity probe against MongoDB.
     *
     * <p>Executes a single-document {@code findAll} with a page size of 1.
     * Returns {@code false} immediately when MongoDB is disabled or the probe
     * throws any exception, so callers can safely branch without try-catch.
     *
     * @return {@code true} if MongoDB is enabled and reachable; {@code false} otherwise
     */
    public boolean isAvailable() {
        if (!mongoEnabled) {
            return false;
        }
        try {
            listingMetadataRepository.findAll(PageRequest.of(0, 1));
            return true;
        } catch (Exception ex) {
            log.warn("[MongoDB] availability check failed: {}", ex.getMessage());
            return false;
        }
    }
}

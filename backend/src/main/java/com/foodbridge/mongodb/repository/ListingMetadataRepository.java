package com.foodbridge.mongodb.repository;

import com.foodbridge.mongodb.document.ListingMetadata;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for {@link ListingMetadata} documents.
 * Only active when foodbridge.mongodb.enabled=true and a MongoDB instance
 * is reachable; all callers must go through {@code MongoMetadataService}
 * which guards every call.
 */
@Repository
public interface ListingMetadataRepository extends MongoRepository<ListingMetadata, String> {
    Optional<ListingMetadata> findByMysqlListingId(Long mysqlListingId);
    void deleteByMysqlListingId(Long mysqlListingId);
}

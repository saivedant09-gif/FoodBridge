// FoodBridge MongoDB Collection Design and Indexes
// Run with: mongosh foodbridge_meta < indexes.js

// =====================================================
// Collection: listing_metadata
// Supplementary optional metadata for food listings.
// Core listing data is in MySQL food_listings table.
// =====================================================

db.listing_metadata.createIndex(
    { mysqlListingId: 1 },
    { unique: true, name: "idx_listing_metadata_mysql_id" }
);

db.listing_metadata.createIndex(
    { requiresRefrigeration: 1 },
    { name: "idx_listing_metadata_refrigeration" }
);

// =====================================================
// Collection: organization_profiles
// Supplementary flexible profile data for organizations.
// Core org data is in MySQL organizations table.
// =====================================================

db.organization_profiles.createIndex(
    { mysqlOrgId: 1 },
    { unique: true, name: "idx_org_profiles_mysql_id" }
);

print("FoodBridge MongoDB indexes created successfully.");

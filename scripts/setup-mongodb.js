// =============================================================
// FoodBridge — MongoDB Setup Script
// Usage: mongosh foodbridge_meta < scripts/setup-mongodb.js
// =============================================================

const dbName = 'foodbridge_meta';
db = db.getSiblingDB(dbName);

print('Setting up FoodBridge MongoDB database: ' + dbName);

// 1. Create collections
db.createCollection('listing_metadata');
db.createCollection('organization_profiles');

// 2. Create indexes
db.listing_metadata.createIndex(
    { mysqlListingId: 1 },
    { unique: true, name: 'idx_listing_metadata_mysql_id' }
);

db.listing_metadata.createIndex(
    { requiresRefrigeration: 1 },
    { name: 'idx_listing_metadata_refrigeration' }
);

db.organization_profiles.createIndex(
    { mysqlOrgId: 1 },
    { unique: true, name: 'idx_org_profiles_mysql_id' }
);

// 3. Seed demonstration document
db.listing_metadata.updateOne(
    { mysqlListingId: 1 },
    {
        $set: {
            mysqlListingId: 1,
            imagePaths: ['/assets/images/veg_biryani.jpg'],
            handlingInstructions: ['Keep hot above 65°C', 'Serve within 3 hours'],
            additionalAttributes: {
                cuisine: 'South Indian',
                spiciness: 'Medium',
                packaging: 'Sealed foil trays'
            },
            storageInstructions: 'Hot holding insulated container',
            packagingNotes: 'Individual meal box portions',
            requiresRefrigeration: false,
            createdAt: new Date(),
            updatedAt: new Date()
        }
    },
    { upsert: true }
);

print('FoodBridge MongoDB initialization completed successfully.');

# FoodBridge — MongoDB Collection Design Document
## Supplementary Document Store Architecture

---

## 1. Architectural Role & Boundary

In FoodBridge, **MySQL is the sole authoritative system of record** for all transactional, inventory, authorization, and lifecycle data. 

**MongoDB is integrated as a supplementary document store** specifically for:
1. Flexible, unstructured operational metadata that varies between food categories.
2. Extended organization profiles and qualitative program narratives.
3. Media references (images, inspection photo paths, packaging notes).

### Core Architectural Guarantees:
- **No Dual Inventory:** Authoritative `available_quantity` and allocation records reside strictly in MySQL `food_listings` and `allocations`.
- **Fault Tolerance & Non-Blocking Design:** If MongoDB is offline, unavailable, or misconfigured, every MySQL transactional workflow operates with 100% functionality. MongoDB operations are wrapped in safe fallback blocks via `MongoMetadataService`.
- **Foreign Key Referencing:** MongoDB documents reference the primary relational entity via `mysqlListingId` or `mysqlOrgId`.

---

## 2. Collections Specification

### Collection 1: `listing_metadata`
Stores flexible attributes for food listings that do not warrant relational schema alteration.

```json
{
  "_id": ObjectId("6525f1a2e3b1c8290f110001"),
  "mysqlListingId": 1,
  "imagePaths": [
    "/assets/images/listing_1_front.jpg",
    "/assets/images/listing_1_label.jpg"
  ],
  "handlingInstructions": [
    "Keep covered in insulated container",
    "Consume within 3 hours of collection"
  ],
  "additionalAttributes": {
    "servings_per_tray": "25",
    "cuisine_style": "South Indian",
    "spiciness_level": "Medium",
    "packaging_material": "Food-grade aluminum container"
  },
  "storageInstructions": "Hot holding above 65°C recommended",
  "packagingNotes": "Sealed with tamper-evident film",
  "requiresRefrigeration": false,
  "createdAt": "2026-10-11T12:00:00Z",
  "updatedAt": "2026-10-11T12:00:00Z"
}
```

#### Indexes:
- `db.listing_metadata.createIndex({ mysqlListingId: 1 }, { unique: true })`
- `db.listing_metadata.createIndex({ requiresRefrigeration: 1 })`

---

### Collection 2: `organization_profiles`
Stores supplementary qualitative profile information for recipient and provider organizations.

```json
{
  "_id": ObjectId("6525f1a2e3b1c8290f110002"),
  "mysqlOrgId": 5,
  "socialLinks": {
    "website": "https://vellorehope.org",
    "annual_report": "https://vellorehope.org/reports/2025.pdf"
  },
  "mission": "Eradicating localized hunger through dignified, coordinated redistribution.",
  "servicesOffered": [
    "Elderly hot meal distribution",
    "Night shelter food support",
    "Weekend community kitchen"
  ],
  "operatingHours": "08:00 - 21:00 Daily",
  "beneficiariesServedMonthly": 450,
  "certifications": [
    "FSSAI Registration No. 22421000000000",
    "Section 12A Charitable Trust",
    "Section 80G Tax Exemption"
  ],
  "createdAt": "2026-01-11T09:30:00Z",
  "updatedAt": "2026-01-11T09:30:00Z"
}
```

#### Indexes:
- `db.organization_profiles.createIndex({ mysqlOrgId: 1 }, { unique: true })`

---

## 3. Comparison Matrix: MySQL vs. MongoDB Responsibilities

| Dimension | MySQL 8.0 (Relational) | MongoDB 7.0 (Document) |
|-----------|------------------------|------------------------|
| **Primary Responsibility** | Authoritative transactions, inventory, users, requests, allocations | Optional supplementary attributes, documents, qualitative metadata |
| **Integrity Enforcement** | Foreign keys, constraints (`CHECK`, `UNIQUE`), InnoDB row locking | Application-level validation, flexible schema |
| **Concurrency Control** | `SERIALIZABLE` isolation, row-level locks, atomic conditional updates | Document-level atomic updates |
| **System Availability Impact** | Mandatory; platform cannot operate without MySQL | Optional; system gracefully degrades if MongoDB is down |

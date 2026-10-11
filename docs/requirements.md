# FoodBridge — Software Requirements Specification (SRS)
## Functional and Non-Functional Requirements Document

---

## 1. Stakeholders & User Roles

| Role | Target Users | Primary Responsibilities |
|------|-------------|---------------------------|
| **Provider** | Restaurants, university dining halls, bakeries, caterers, banquet halls | Publish surplus food, review claims, approve allocations, confirm physical handover |
| **Recipient** | NGOs, community kitchens, charitable trusts, homeless shelters | Discover available food, submit claim requests, organize logistics, confirm receipt |
| **Administrator** | University platform managers, system supervisors | Verify registered organizations, monitor audit trail, handle complaints/issues, review metrics |

---

## 2. Functional Requirements (FR)

### Module 1: Authentication & Account Management
- **FR-01.1 Registration:** Public users can register as `PROVIDER` or `RECIPIENT` alongside their organization profile. Public registration as `ADMIN` is strictly rejected.
- **FR-01.2 Credential Protection:** Passwords must be hashed using BCrypt (work factor 12). Plaintext passwords must never be persisted or logged.
- **FR-01.3 Session Security:** Authenticated sessions use secure HTTP sessions stored server-side.
- **FR-01.4 Authorization:** Role-based access control (RBAC) enforced on every endpoint (`@PreAuthorize` / Spring Security filter chain).

### Module 2: Organization Governance & Verification
- **FR-02.1 Profile Creation:** Organizations record legal name, type, contact information, physical address, and coordinates.
- **FR-02.2 Verification Gate:** Recipient organizations must be in `APPROVED` status before being permitted to submit food requests.
- **FR-02.3 Administrative Verification:** Administrators can review pending organizations and update status to `APPROVED` or `REJECTED`, logging the decision to the audit log.

### Module 3: Food Listing Management
- **FR-03.1 Listing Creation:** Providers can list food specifying category, total quantity, unit, preparation time, expiry timestamp, pickup deadline, pickup address, and dietary tags.
- **FR-03.2 Date Validation:** Listings must satisfy `expires_at >= pickup_deadline`.
- **FR-03.3 Quantity Consistency:** `available_quantity` is initialized to `total_quantity` and constrained by database CHECK constraints: `0 <= available_quantity <= total_quantity`.
- **FR-03.4 Lifecycle:** Status moves through `ACTIVE`, `FULLY_ALLOCATED`, `EXPIRED`, `CANCELLED`, `COMPLETED`.

### Module 4: Request & Claim Management
- **FR-04.1 Claim Submission:** Verified recipients submit requests with positive quantity (`requested_quantity > 0`).
- **FR-04.2 Duplicate Prevention:** The platform prevents duplicate active requests by the same recipient for the same listing.
- **FR-04.3 Provider Review:** Providers review incoming pending requests and approve or reject them.

### Module 5: Transaction-Safe Allocation Engine
- **FR-05.1 Concurrency Safety:** Allocation operations use Spring `@Transactional(isolation = Isolation.SERIALIZABLE)` and atomic conditional SQL updates:
  ```sql
  UPDATE food_listings 
  SET available_quantity = available_quantity - ? 
  WHERE listing_id = ? AND available_quantity >= ? AND listing_status = 'ACTIVE';
  ```
- **FR-05.2 Rollback on Failure:** If the atomic update returns 0 affected rows, an `InsufficientFoodException` is thrown, causing an immediate database transaction rollback.
- **FR-05.3 Record Synchronization:** Allocation creation, request status update to `APPROVED`, collection record initialization, and inventory decrement occur in a single atomic transaction.

### Module 6: Two-Party Collection Confirmation
- **FR-06.1 Handover Confirmation:** Providers confirm physical handover (`provider_confirmed = 1`).
- **FR-06.2 Receipt Confirmation:** Recipients confirm physical receipt (`recipient_confirmed = 1`).
- **FR-06.3 Fulfillment Transition:** When both parties have confirmed, status transitions to `COLLECTED`, allocation marks `COMPLETED`, and request marks `FULFILLED` via database triggers and service logic.

### Module 7: Intelligent Matching & Urgency Scoring
- **FR-07.1 Rule-Based Ranking:** Transparent weighted score (0–100) combining category match (25%), quantity suitability (20%), collection feasibility (20%), recipient verification (20%), and time urgency (15%).
- **FR-07.2 Explainability:** Every recommendation provides an explicit mathematical breakdown string to the user.

### Module 8: Audit Logging & Issue Management
- **FR-08.1 Immutable Trail:** Critical actions (`USER_REGISTERED`, `FOOD_LISTING_CREATED`, `ALLOCATION_CONFIRMED`, `ORG_VERIFIED`, etc.) are recorded with actor ID, timestamp, entity type, and metadata.
- **FR-08.2 Problem Reporting:** Users can flag quality discrepancies or missed pickups as structured issues for administrative review.

---

## 3. Non-Functional Requirements (NFR)

1. **Data Integrity:** Enforced via InnoDB foreign keys, primary keys, and CHECK constraints.
2. **Transaction Isolation:** High-contention inventory updates use SERIALIZABLE isolation to eliminate dirty reads, non-repeatable reads, and phantom reads.
3. **Graceful Fallback:** If MongoDB is offline, all core relational operations operate with 100% functionality without throwing exceptions to end users.
4. **Performance:** Critical lookup columns (`users.email`, `food_listings.category`, `food_listings.expires_at`, `food_listings.pickup_deadline`) are indexed for sub-10ms response times.
5. **Responsiveness:** Mobile-responsive frontend built with Bootstrap 5 grid layout.

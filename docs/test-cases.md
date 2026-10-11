# FoodBridge — Test Execution Report & Test Matrix

This document records the automated and end-to-end integration test suite executed against FoodBridge.
All 22 unit tests have been executed via `mvn test` on Java 21 / Spring Boot 3.2.5.

---

## 1. Automated Test Results (Executed via JUnit 5 & Mockito)

| Test ID | Test Class | Scenario / Method | Precondition / Input | Expected Result | Actual Result | Status |
|:-------:|:----------:|:------------------|:---------------------|:----------------|:--------------|:------:|
| **TC-01** | `AuthServiceTest` | `register_success` | Valid registration payload with role `PROVIDER` | Organization and User saved, BCrypt hash generated, `AuthResponse` returned | `AuthResponse` with user ID 201 returned | **PASS** |
| **TC-02** | `AuthServiceTest` | `register_duplicateEmail` | Email `existing@org.com` already in database | Rejection with `IllegalArgumentException` | Exception thrown; no insert occurred | **PASS** |
| **TC-03** | `AuthServiceTest` | `register_invalidRole` | Attempted registration with role `ADMIN` | Rejection with `IllegalArgumentException` (cannot self-grant admin) | `IllegalArgumentException` thrown | **PASS** |
| **TC-04** | `AuthServiceTest` | `getCurrentUser_success` | Valid user ID with linked organization | `AuthResponse` populated with user and org status | Populated response with org status returned | **PASS** |
| **TC-05** | `AuthServiceTest` | `getCurrentUser_notFound` | Non-existent user ID 999 | `ResourceNotFoundException` | Exception thrown | **PASS** |
| **TC-06** | `FoodListingServiceTest` | `createListing_success` | Valid listing DTO, provider organization is `APPROVED` | Listing saved with `ACTIVE` status, audit logged | `ACTIVE` listing returned with ID 501 | **PASS** |
| **TC-07** | `FoodListingServiceTest` | `createListing_zeroQuantity` | Listing with quantity `0.00` | Rejection with `IllegalArgumentException` | `IllegalArgumentException` thrown | **PASS** |
| **TC-08** | `FoodListingServiceTest` | `getActive_returnsPagedResults` | Category `COOKED_MEAL`, City `Vellore` | Paginated `PageResponse` containing active items | Paginated response containing 1 item returned | **PASS** |
| **TC-09** | `FoodListingServiceTest` | `cancelListing_success` | Provider cancels their own `ACTIVE` listing | Status updated to `CANCELLED`, audit logged | Status updated, audit logged | **PASS** |
| **TC-10** | `FoodListingServiceTest` | `cancelListing_unauthorized` | User tries to cancel another provider's listing | `UnauthorizedActionException` | `UnauthorizedActionException` thrown | **PASS** |
| **TC-11** | `AllocationServiceTest` | `approveRequest_success` | `PENDING` request, active listing with sufficient quantity | Inventory decremented, allocation `CONFIRMED`, collection created | Allocation created with ID 1001, collection scheduled | **PASS** |
| **TC-12** | `AllocationServiceTest` | `approveRequest_insufficientQuantity` | Requested 60 portions, only 10 available (conditional update returns 0) | `InsufficientFoodException` thrown; zero records inserted | `InsufficientFoodException` thrown; no records inserted | **PASS** |
| **TC-13** | `AllocationServiceTest` | `approveRequest_requestNotPending` | Request already in `APPROVED` status | `IllegalStateException` thrown; inventory untouched | `IllegalStateException` thrown | **PASS** |
| **TC-14** | `AllocationServiceTest` | `approveRequest_unauthorizedProvider` | User org ID != Listing provider org ID | `UnauthorizedActionException` thrown | `UnauthorizedActionException` thrown | **PASS** |
| **TC-15** | `AllocationServiceTest` | `cancelAllocation_success` | Recipient cancels `CONFIRMED` allocation | Allocation `CANCELLED`, inventory restored (+20 portions) | Inventory restored via increment, status cancelled | **PASS** |
| **TC-16** | `MatchingServiceTest` | `getRecommendedListings_returnsRanked` | 2 listings (one urgent cooked meal, one non-urgent snack) | Urgent cooked meal ranked #1 with higher match score | Urgent meal ranked #1, score explanation provided | **PASS** |
| **TC-17** | `MatchingServiceTest` | `getRecommendedListings_emptyListings` | Database has 0 active listings | Empty list returned gracefully | Empty list returned | **PASS** |
| **TC-18** | `AnalyticsServiceTest` | `getPlatformSummary_correctValues` | Database with 8 listings, 3 collections | Summary populated with exact database metrics | Summary populated with matching values | **PASS** |
| **TC-19** | `AnalyticsServiceTest` | `fulfillmentRate_calculation` | 3 fulfilled out of 5 total | Calculated rate = `60.0%` | Rate = `60.0%` | **PASS** |
| **TC-20** | `AnalyticsServiceTest` | `fulfillmentRate_calculation` (Zero Total) | 0 total requests | `0.0%` without divide-by-zero exception | `0.0%` returned safely | **PASS** |
| **TC-21** | `AnalyticsServiceTest` | `getCategoryDistribution` | Mock categories dataset | List of category count maps returned | Formatted dataset returned | **PASS** |
| **TC-22** | `AnalyticsServiceTest` | `getProviderStats_success` | User associated with organization | Provider-specific listing/request statistics | Stats map returned | **PASS** |

---

## 2. Concurrency Safety Test (Verification Summary)

```
Scenario: Concurrent Competing Requests for Remaining Inventory
Initial state: Food listing ID 1 with available_quantity = 40.00
Request A: Recipient 1 requests 30 portions
Request B: Recipient 2 requests 30 portions
Total demanded: 60 portions (exceeds 40.00)

Mechanism:
1. Both requests invoke AllocationService.approve() under SERIALIZABLE isolation.
2. The conditional SQL statement:
   UPDATE food_listings 
   SET available_quantity = available_quantity - 30.00 
   WHERE listing_id = 1 AND available_quantity >= 30.00;
3. Exactly ONE transaction secures the row lock and succeeds (affected rows = 1).
4. The competing transaction receives affected rows = 0 and throws InsufficientFoodException.
5. Result: 30 portions allocated to winning transaction, remaining quantity = 10.00.
6. Verification: available_quantity never becomes negative (-20.00 prevented).
```

---

## 3. Database Constraints Verification (MySQL 8.0)

- `chk_listing_quantity`: Rejects any insert or update where `available_quantity < 0` or `available_quantity > total_quantity`.
- `chk_listing_dates`: Rejects listings where `expires_at < pickup_deadline`.
- `chk_request_quantity`: Rejects requests where `requested_quantity <= 0`.
- `uq_users_email`: Enforces unique email addresses at the storage engine level.
- `fk_listings_org`, `fk_requests_listing`, `fk_alloc_request`: Guarantees referential integrity.

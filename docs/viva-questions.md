# FoodBridge — Viva Questions & Answers

> This document prepares you to confidently answer technical questions about the FoodBridge project during a viva examination. Each answer references the actual implementation.

---

## Q1. Why did you choose FoodBridge as your project?

**Answer:**

Food waste is one of the largest contributors to global resource inefficiency — approximately one-third of all food produced is wasted, while millions suffer from food insecurity. Redistributing surplus food from providers (restaurants, supermarkets, farms) to recipients (NGOs, food banks, shelters) is a logistical problem that is perfectly suited to a database-driven web application.

FoodBridge was chosen because:
1. It is a **real-world problem** with clear stakeholders, data relationships, and business rules.
2. The allocation workflow (preventing double-booking of limited food stock) is a genuine **concurrency control challenge**, ideal for demonstrating ACID transactions.
3. It involves **multiple user roles** (PROVIDER, RECIPIENT, ADMIN), which requires role-based access control — a key database application design concept.
4. The domain naturally produces rich **analytics** (food saved, categories, monthly trends), showcasing aggregate SQL queries.

---

## Q2. What problem does it solve?

**Answer:**

FoodBridge addresses three problems simultaneously:

| Problem | FoodBridge Solution |
|---------|-------------------|
| Surplus food goes to waste because providers have no efficient way to find recipients | A **searchable, filterable listing system** (`food_listings` table) allows providers to post surplus and recipients to discover it |
| Manual phone/email coordination is slow and error-prone | An end-to-end **request → allocation → collection workflow** automates the entire handoff process |
| No accountability or audit trail exists | An **audit log**, **two-party collection confirmation**, and **issue reporting system** create accountability at every step |

The system specifically solves the **double-allocation problem** — ensuring that the same food stock cannot be promised to two different recipients simultaneously — using conditional SQL `UPDATE` inside a transaction.

---

## Q3. Why did you choose MySQL?

**Answer:**

MySQL was chosen as the primary database for the following reasons:

1. **ACID compliance**: MySQL with the InnoDB storage engine provides full ACID (Atomicity, Consistency, Isolation, Durability) transactions, which are essential for the allocation workflow.

2. **Relational data model**: The domain has clearly defined entities with relationships (organizations → listings → requests → allocations → collections). A relational model with foreign keys enforces these relationships at the database level.

3. **Referential integrity**: MySQL's foreign key constraints prevent orphaned records (e.g., a request for a non-existent listing).

4. **Maturity and tooling**: MySQL 8.0 is well-documented, widely used in production, and supports modern features like window functions, CTEs, and JSON columns.

5. **Java integration**: Spring's `JdbcTemplate` and `DataSource` work seamlessly with MySQL via the `mysql-connector-j` JDBC driver.

**Implementation**: The schema is defined in `d:\FootBridge\backend\src\main\resources\schema.sql`, loaded automatically on startup.

---

## Q4. Why use MongoDB as well?

**Answer:**

MongoDB serves a **complementary role** to MySQL in FoodBridge, not a replacement:

| Concern | Why MongoDB |
|---------|-------------|
| Analytics aggregations | MongoDB's aggregation pipeline is well-suited for computing complex multi-stage aggregations (e.g., monthly food saved by category) without impacting OLTP performance on MySQL |
| Flexible metadata | Extended listing attributes (custom tags, provider notes, rich descriptions) can vary by listing type and fit MongoDB's schema-less document model |
| Audit snapshots | Storing full JSON snapshots of state changes (before/after) is natural in MongoDB's document format |

**Graceful fallback**: If MongoDB is unavailable, the application continues to function. Services in `com.foodbridge.service` return empty results or fall back to MySQL-computed equivalents. The MongoDB beans are marked `@ConditionalOnProperty`, and `MongoAutoConfiguration` is excluded when the host is unreachable. This is handled in `MongoConfig.java`.

---

## Q5. What is normalization?

**Answer:**

Normalization is the process of organizing a database schema to **reduce data redundancy** and **improve data integrity** by applying a series of rules called Normal Forms.

**Applied to FoodBridge:**

| Normal Form | Rule | FoodBridge Example |
|------------|------|--------------------|
| **1NF** | Each column holds atomic (indivisible) values; no repeating groups | `food_listings.category` stores one category string, not a comma-separated list |
| **2NF** | All non-key attributes depend on the *whole* primary key (relevant for composite PKs) | All tables use single surrogate PKs, so 2NF is automatically satisfied |
| **3NF** | No non-key attribute depends on another non-key attribute (no transitive dependency) | `food_requests` does NOT store the recipient's organization name — only `recipient_organization_id`. The name is fetched from `organizations` to avoid update anomalies |

**Why it matters**: If the organization's name were stored in every `food_request` row, changing the organization's name would require updating thousands of rows. With normalization, only one row in `organizations` changes.

---

## Q6. Explain the ER diagram.

**Answer:**

The FoodBridge ER diagram has the following key entities and relationships:

```
ORGANIZATIONS (1) ─────────────────────── (M) FOOD_LISTINGS
     │                                          │
     └──── (1) ──── (M) FOOD_REQUESTS ─────────┘
                          │
                          │ (1:1)
                          ▼
                     ALLOCATIONS
                          │
                          │ (1:1)
                          ▼
                     COLLECTIONS
```

**Cardinalities**:
- One **Organization** can post many **Food Listings** (as a provider).
- One **Organization** can submit many **Food Requests** (as a recipient).
- One **Food Listing** can receive many **Food Requests** (multiple recipients can request the same listing).
- One **Food Request** maps to exactly one **Allocation** (enforced by `UNIQUE(request_id)` in `allocations`).
- One **Allocation** maps to exactly one **Collection** (enforced by `UNIQUE(allocation_id)` in `collections`).
- **Users** belong to one Organization and receive many **Notifications**.
- **Audit Log** entries are associated with the User who triggered them.

The one-to-one relationships (Request→Allocation, Allocation→Collection) model the strict sequential lifecycle of food redistribution.

---

## Q7. What are primary and foreign keys?

**Answer:**

**Primary Key (PK)**: A column (or set of columns) that **uniquely identifies each row** in a table. It cannot be NULL and must be unique.

In FoodBridge, every table has a single-column surrogate integer PK:
```sql
listing_id INT AUTO_INCREMENT PRIMARY KEY
```

**Foreign Key (FK)**: A column in one table that **references the primary key of another table**, enforcing referential integrity — you cannot have a food request pointing to a non-existent listing.

Example from FoodBridge:
```sql
-- In food_requests:
listing_id INT NOT NULL,
FOREIGN KEY (listing_id) REFERENCES food_listings(listing_id)
```

**Cascade behavior in FoodBridge**: Most foreign keys use `ON DELETE RESTRICT` — you cannot delete a listing that has requests, preserving historical integrity. Organizations and users are never deleted; they are **suspended** instead.

---

## Q8. How does JDBC connect Java to MySQL?

**Answer:**

**JDBC (Java Database Connectivity)** is the standard Java API for relational database access. It provides a set of interfaces that database vendors implement via **drivers**.

The connection chain in FoodBridge:

```
Spring Boot Application
     │
     ▼
application.properties
  spring.datasource.url=jdbc:mysql://localhost:3306/foodbridge
  spring.datasource.username=root
  spring.datasource.password=...
     │
     ▼
HikariCP Connection Pool (auto-configured by Spring Boot)
     │  (maintains a pool of physical TCP connections to MySQL)
     ▼
mysql-connector-j (JDBC Driver, in pom.xml)
     │  (translates JDBC calls to MySQL wire protocol)
     ▼
MySQL 8.0 Server (port 3306)
```

The JDBC driver (`com.mysql.cj.jdbc.Driver`) is declared as a Maven dependency in `pom.xml`. Spring Boot's auto-configuration reads `application.properties` and creates a `DataSource` bean automatically.

---

## Q9. What is the purpose of JdbcTemplate?

**Answer:**

`JdbcTemplate` is a Spring class that **eliminates boilerplate JDBC code**. Without it, every database operation requires:
1. Getting a `Connection` from the pool
2. Creating a `PreparedStatement`
3. Setting parameters one by one
4. Executing the query
5. Iterating over `ResultSet`
6. Closing the statement and connection (even on exceptions)

**JdbcTemplate handles all of this automatically:**

```java
// Without JdbcTemplate (manual JDBC) — ~20 lines
Connection conn = dataSource.getConnection();
PreparedStatement ps = conn.prepareStatement("SELECT * FROM food_listings WHERE listing_id = ?");
ps.setInt(1, id);
ResultSet rs = ps.executeQuery();
// ... iterate, map, close ...

// With JdbcTemplate — 3 lines
return jdbcTemplate.queryForObject(
    "SELECT * FROM food_listings WHERE listing_id = ?",
    listingRowMapper, id);
```

In FoodBridge, `JdbcTemplate` is injected into every repository class:
- `FoodListingRepository` — listing CRUD and status updates
- `FoodRequestRepository` — request management
- `AllocationRepository` — allocation approval with conditional updates
- `UserRepository` — user lookup and status management

`RowMapper` implementations (e.g., `ListingRowMapper`) map `ResultSet` columns to Java objects.

---

## Q10. How do joins work in the application?

**Answer:**

SQL JOINs combine rows from two or more tables based on related columns. FoodBridge uses joins extensively to fetch contextual data in a single query.

**Example — Listing with provider name:**
```sql
SELECT fl.*, o.name AS provider_name, o.address AS provider_address
FROM food_listings fl
JOIN organizations o ON fl.provider_organization_id = o.organization_id
WHERE fl.status = 'ACTIVE'
```

**Example — Allocation with full context (request + listing + recipient org):**
```sql
SELECT a.*, fr.requested_quantity, fr.notes,
       fl.title AS listing_title, fl.category,
       o.name AS recipient_name
FROM allocations a
JOIN food_requests fr ON a.request_id = fr.request_id
JOIN food_listings fl ON a.listing_id = fl.listing_id
JOIN organizations o ON fr.recipient_organization_id = o.organization_id
WHERE a.allocation_id = ?
```

In FoodBridge, these multi-table queries are written as SQL strings in the repository classes (e.g., `AllocationRepository.java`) and executed via `JdbcTemplate.query()`. The results are mapped using `RowMapper` implementations.

---

## Q11. What are ACID properties?

**Answer:**

ACID is a set of properties that guarantee database transactions are processed reliably:

| Property | Definition | FoodBridge Example |
|----------|-----------|-------------------|
| **Atomicity** | A transaction is all-or-nothing. If any step fails, the entire transaction rolls back. | In allocation approval: if inserting the `allocation` row fails, the `available_quantity` update also rolls back. No partial state is left. |
| **Consistency** | A transaction takes the database from one valid state to another, respecting all constraints. | `available_quantity` can never go below 0 (CHECK constraint). The allocation transaction ensures this. |
| **Isolation** | Concurrent transactions cannot see each other's intermediate states. | Two simultaneous allocation approvals for the same listing serialize at the `UPDATE food_listings` row lock. Only one sees `available_quantity >= requested`. |
| **Durability** | Once committed, changes survive crashes. | MySQL InnoDB writes transactions to the redo log before acknowledging commit. Data persists even if the server crashes immediately after. |

---

## Q12. How do transactions prevent inconsistent allocations?

**Answer:**

The `AllocationService.approveAllocation()` method in `com.foodbridge.service.AllocationService` uses Spring's `TransactionTemplate` to wrap the entire approval workflow in a single transaction:

```java
return transactionTemplate.execute(status -> {
    // Step 1: Fetch and validate request
    FoodRequest request = requestRepo.findById(requestId);
    if (!request.getStatus().equals("PENDING")) throw new RuntimeException("Not pending");

    // Step 2: Conditionally reduce available quantity
    int rows = jdbcTemplate.update(
        "UPDATE food_listings SET available_quantity = available_quantity - ? " +
        "WHERE listing_id = ? AND available_quantity >= ?",
        request.getRequestedQuantity(), request.getListingId(), request.getRequestedQuantity()
    );
    if (rows == 0) throw new InsufficientQuantityException();

    // Step 3: Create allocation record
    allocationRepo.insert(new Allocation(...));

    // Step 4: Update request status
    requestRepo.updateStatus(requestId, "APPROVED");

    // Step 5: Notify recipient
    notificationService.create(...);

    return allocation; // COMMIT
});
// Any exception above → automatic ROLLBACK
```

If any step throws an exception, Spring automatically calls `ROLLBACK`, restoring `available_quantity` to its original value.

---

## Q13. How do you prevent double allocation?

**Answer:**

Double allocation is prevented by a combination of two mechanisms:

**Mechanism 1 — Conditional UPDATE (database-level lock):**
```sql
UPDATE food_listings
SET available_quantity = available_quantity - :qty
WHERE listing_id = :id AND available_quantity >= :qty
```
This single statement is **atomic at the InnoDB level**. It acquires a row-level exclusive lock before checking and updating. If two concurrent transactions execute this simultaneously for the same listing:
- Transaction A acquires the lock, sees `available_quantity = 10`, deducts 8 → succeeds (available = 2).
- Transaction B waits for the lock, acquires it, sees `available_quantity = 2`, checks `2 >= 8` → FALSE → `rowsAffected = 0` → throws `InsufficientQuantityException` → rolls back.

**Mechanism 2 — UNIQUE constraint:**
```sql
UNIQUE KEY uq_allocation_request (request_id)
```
Even if the application layer allowed it, the database enforces that only one allocation can ever exist per request.

---

## Q14. What is concurrency control?

**Answer:**

Concurrency control manages simultaneous access to shared data by multiple transactions to prevent anomalies.

**Types and FoodBridge's approach:**

| Strategy | Description | Used in FoodBridge |
|----------|-------------|-------------------|
| **Pessimistic locking** | Lock the row before reading so no other transaction can modify it | Implicitly used: InnoDB's `UPDATE` acquires an exclusive row lock |
| **Optimistic locking** | Read without locking; check at write time if data changed | Used via `WHERE available_quantity >= ?` — the write is conditional |
| **Serializable isolation** | Transactions execute as if they were sequential | Not needed; row-level locking at the `UPDATE` achieves the same effect for the critical path |

The allocation approval uses a hybrid: **optimistic locking during the read phase** (no explicit `SELECT FOR UPDATE`) and **implicit pessimistic locking during the UPDATE phase** (InnoDB row lock). This maximizes concurrency while preventing double allocation.

---

## Q15. Why are indexes useful?

**Answer:**

An index is a data structure (typically a B-tree) that allows the database to find rows matching a condition **without scanning the entire table**. Without an index, MySQL must examine every row (full table scan), which becomes very slow as data grows.

**FoodBridge example:**
```sql
-- Without index on status: scans ALL rows in food_listings
SELECT * FROM food_listings WHERE status = 'ACTIVE';

-- With idx_listings_status: jumps directly to 'ACTIVE' rows via B-tree
-- Time complexity: O(log n) instead of O(n)
```

**Key indexes in FoodBridge:**

| Index | Why Critical |
|-------|-------------|
| `users.email` | Login check runs on every request; must be instant |
| `notifications.(user_id, is_read)` | Unread badge count loads on every page load for every user |
| `food_listings.status` | The most common filter on the main listings page |
| `food_requests.listing_id` | Finding all requests for a listing when a provider views their dashboard |

The tradeoff: indexes consume disk space and slow down `INSERT`/`UPDATE` operations (index must be maintained). FoodBridge indexes are chosen because reads vastly outnumber writes.

---

## Q16. What is the purpose of EXPLAIN?

**Answer:**

`EXPLAIN` is a MySQL command that shows how the query optimizer will **execute a query** — which indexes it uses, how many rows it estimates scanning, and what join strategy it chooses.

```sql
EXPLAIN SELECT * FROM food_listings WHERE status = 'ACTIVE' AND category = 'Produce';
```

**Output columns to examine:**

| Column | Meaning |
|--------|---------|
| `type` | Access method: `const` (best) > `ref` > `range` > `ALL` (worst = full scan) |
| `key` | Which index MySQL chose |
| `rows` | Estimated rows examined |
| `Extra` | Additional info: `Using index`, `Using filesort` (bad), `Using temporary` (bad) |

In FoodBridge development, `EXPLAIN` was used to confirm that:
- `idx_listings_status` was being used for the active listings query.
- `idx_notifications_user_read` was used for the unread count query (`SELECT COUNT(*) WHERE user_id = ? AND is_read = 0`).

---

## Q17. What are views?

**Answer:**

A **view** is a saved SQL query that appears as a virtual table. It does not store data itself — it executes the underlying query when accessed.

**Benefits:**
- **Simplification**: Encapsulates complex joins; application code selects from a view instead of writing the join every time.
- **Security**: Grant access to a view without exposing the base tables.
- **Consistency**: All code uses the same query definition; no risk of slight differences between copies.

**FoodBridge views:**

```sql
-- v_active_listings: Only shows non-expired, non-cancelled listings
CREATE VIEW v_active_listings AS
SELECT fl.*, o.name AS provider_name
FROM food_listings fl
JOIN organizations o ON fl.provider_organization_id = o.organization_id
WHERE fl.status = 'ACTIVE' AND fl.expiry_date > NOW();

-- Usage in application:
SELECT * FROM v_active_listings WHERE category = ?;
```

---

## Q18. What are triggers?

**Answer:**

A **trigger** is a database procedure that executes **automatically** in response to an `INSERT`, `UPDATE`, or `DELETE` event on a table.

**FoodBridge triggers:**

```sql
-- Trigger 1: Auto-complete collection when both parties confirm
CREATE TRIGGER trg_collection_complete
AFTER UPDATE ON collections
FOR EACH ROW
BEGIN
    IF NEW.provider_confirmed = 1 AND NEW.recipient_confirmed = 1 THEN
        UPDATE collections SET status = 'COMPLETED' WHERE collection_id = NEW.collection_id;
        UPDATE allocations SET status = 'COMPLETED'
        WHERE allocation_id = NEW.allocation_id;
    END IF;
END;

-- Trigger 2: Audit user status changes
CREATE TRIGGER trg_audit_user_status
AFTER UPDATE ON users
FOR EACH ROW
BEGIN
    IF OLD.status != NEW.status THEN
        INSERT INTO audit_log (entity_type, entity_id, action, old_values, new_values, created_at)
        VALUES ('USER', NEW.user_id, 'STATUS_CHANGE',
                JSON_OBJECT('status', OLD.status),
                JSON_OBJECT('status', NEW.status), NOW());
    END IF;
END;
```

Triggers enforce business rules at the database level, even if the application layer is bypassed.

---

## Q19. What are stored procedures?

**Answer:**

A **stored procedure** is a named, reusable SQL program stored in the database that can be called by name with parameters.

**Benefits over inline SQL:**
- Reduces network round-trips (one `CALL` instead of multiple queries).
- Encapsulates complex logic in the database tier.
- Can be granted execute permission independently of table access.

**FoodBridge stored procedures:**

```sql
-- Procedure: Expire overdue listings in bulk
CREATE PROCEDURE sp_expire_listings(IN cutoff DATETIME)
BEGIN
    UPDATE food_listings
    SET status = 'EXPIRED'
    WHERE status IN ('ACTIVE', 'PARTIALLY_ALLOCATED')
      AND pickup_deadline < cutoff;
END;

-- Called by Spring scheduled task:
-- jdbcTemplate.execute("CALL sp_expire_listings(NOW())");

-- Procedure: Provider statistics for a date range
CREATE PROCEDURE sp_provider_stats(IN org_id INT, IN start_date DATE, IN end_date DATE)
BEGIN
    SELECT
        COUNT(DISTINCT fl.listing_id) AS total_listings,
        SUM(a.allocated_quantity) AS total_allocated_kg,
        COUNT(DISTINCT fr.recipient_organization_id) AS unique_recipients
    FROM food_listings fl
    LEFT JOIN food_requests fr ON fl.listing_id = fr.listing_id
    LEFT JOIN allocations a ON fr.request_id = a.request_id
    WHERE fl.provider_organization_id = org_id
      AND fl.created_at BETWEEN start_date AND end_date;
END;
```

---

## Q20. How is role-based access enforced?

**Answer:**

FoodBridge enforces role-based access control (RBAC) at **three layers**:

**Layer 1 — Spring Security Configuration** (`SecurityConfig.java`):
```java
.requestMatchers("/api/v1/listings/create").hasRole("PROVIDER")
.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
.requestMatchers("/api/v1/requests/**").hasAnyRole("RECIPIENT", "ADMIN")
```
Unauthenticated requests to protected endpoints return `401 Unauthorized`. Wrong-role requests return `403 Forbidden`.

**Layer 2 — Service Layer Validation:**
Even if an endpoint allows a role, the service checks ownership:
```java
// In AllocationService.approveAllocation():
FoodListing listing = listingRepo.findById(request.getListingId());
if (!listing.getProviderOrganizationId().equals(currentUser.getOrganizationId())) {
    throw new AccessDeniedException("You can only approve your own listings");
}
```

**Layer 3 — Session-Based Authentication:**
- Login stores the authenticated user in `HttpSession`.
- `SecurityContextHolder` provides the current user to any service method.
- Sessions expire after 30 minutes of inactivity (configured in `application.properties`).

---

## Q21. What happens if MongoDB is unavailable?

**Answer:**

FoodBridge is designed with **graceful degradation** for MongoDB unavailability:

**Configuration (`MongoConfig.java`):**
```java
@ConditionalOnProperty(name = "mongodb.enabled", havingValue = "true", matchIfMissing = false)
@Configuration
public class MongoConfig { ... }
```

**Auto-configuration exclusion in `application.properties`:**
```properties
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
```
When MongoDB is excluded, the application starts normally — no connection attempts, no startup failure.

**Service-level fallback:**
```java
public Map<String, Object> getAnalytics() {
    try {
        return mongoAnalyticsService.getAggregations();
    } catch (Exception e) {
        log.warn("MongoDB unavailable, falling back to MySQL analytics");
        return mysqlAnalyticsService.getAnalytics(); // fallback
    }
}
```

The core food redistribution workflow (listings, requests, allocations, collections) is entirely MySQL-based and is not affected by MongoDB availability.

---

## Q22. How are analytics calculated?

**Answer:**

Analytics in FoodBridge are computed via **SQL aggregate queries** executed by `AnalyticsService.java`:

**Provider Analytics (MySQL):**
```sql
-- Total food donated (kg)
SELECT SUM(a.allocated_quantity) AS total_donated
FROM food_listings fl
JOIN food_requests fr ON fl.listing_id = fr.listing_id
JOIN allocations a ON fr.request_id = a.request_id
WHERE fl.provider_organization_id = :orgId AND a.status = 'COMPLETED';

-- Listings by category (for pie chart)
SELECT category, COUNT(*) AS listing_count, SUM(total_quantity) AS total_kg
FROM food_listings
WHERE provider_organization_id = :orgId
GROUP BY category;

-- Monthly trend (for line chart)
SELECT YEAR(created_at) AS yr, MONTH(created_at) AS mo,
       SUM(total_quantity) AS quantity_listed
FROM food_listings
WHERE provider_organization_id = :orgId
GROUP BY YEAR(created_at), MONTH(created_at)
ORDER BY yr, mo;
```

**Platform-Wide Analytics (Admin):**
Computed by `sp_platform_summary` stored procedure and returned as a JSON object from `AdminController.java`.

On the frontend, the data from `/api/v1/analytics/provider` is fed to **Chart.js** to render bar charts, pie charts, and line graphs in the provider dashboard.

---

## Q23. What limitations does the project have?

**Answer:**

FoodBridge is a functional prototype with the following acknowledged limitations:

| Limitation | Description |
|-----------|-------------|
| **No real-time communication** | Notifications are pull-based (polling). No WebSocket/SSE push notifications. |
| **No email/SMS integration** | In-app notifications only; no external alerts via email or SMS. |
| **Single-server architecture** | No horizontal scaling; a single JVM handles all requests. |
| **No geospatial queries** | Listings cannot be filtered by proximity/distance to the recipient. |
| **No file uploads** | Providers cannot upload food photos or safety certificates. |
| **Manual expiry** | Expired listings are cleaned up by a scheduled job, not in real-time. |
| **Single currency/unit system** | Units are free-text, not normalized (kg/lbs/boxes may be mixed). |
| **No payment/transport integration** | The system does not coordinate transport — pickup logistics are manual. |
| **MongoDB optional** | Analytics are basic SQL aggregations; the full MongoDB analytics pipeline is not implemented. |

---

## Q24. How could the system scale in the future?

**Answer:**

FoodBridge can scale in the following directions:

**Technical Scaling:**

| Enhancement | Implementation |
|------------|---------------|
| **Horizontal scaling** | Deploy multiple Spring Boot instances behind a **load balancer** (e.g., Nginx). Move session storage from in-memory to **Redis** so any instance can serve any session. |
| **Database read replicas** | Configure MySQL replication; route read queries (listing browsing) to replicas, writes to primary. |
| **Caching** | Add **Redis** caching for active listings (`@Cacheable`). The listing catalog changes infrequently but is read thousands of times per minute. |
| **Async notifications** | Replace synchronous notification inserts with a message queue (**RabbitMQ** or **Kafka**). Notification workers consume events and fan out to email/SMS. |
| **Full-text search** | Index `food_listings.title/description` in **Elasticsearch** for fast keyword search as the catalog grows. |
| **Geospatial matching** | Add GPS coordinates to organizations. Use MySQL's `ST_Distance` or PostGIS to match providers and recipients by proximity. |
| **Mobile app** | The REST API (`/api/v1/**`) is already mobile-ready. A React Native or Flutter app can consume the same endpoints. |
| **Microservices** | Decompose into separate services: ListingService, RequestService, NotificationService — each independently deployable and scalable. |
| **Event sourcing** | Replace status-column updates with an immutable event stream, enabling perfect audit trails and temporal queries. |

# FoodBridge — System Architecture Document

## 1. Overview

FoodBridge follows a **three-tier web architecture** separating presentation, business logic, and data concerns. The tiers communicate exclusively over HTTP/REST, making each tier independently replaceable.

```
┌─────────────────────────────────────────────────────────────┐
│  TIER 1 — PRESENTATION (Frontend)                           │
│  HTML5 + CSS3 + Bootstrap 5 + Vanilla JavaScript            │
│  d:\FootBridge\frontend\                                     │
│  Served by: any static file server / browser file://        │
└──────────────────────────┬──────────────────────────────────┘
                           │ HTTP/REST (JSON)
                           │ /api/v1/**
                           ▼
┌─────────────────────────────────────────────────────────────┐
│  TIER 2 — APPLICATION (Backend)                             │
│  Spring Boot 3.2 / Java 21                                  │
│  d:\FootBridge\backend\                                      │
│  Port: 8080                                                  │
│  com.foodbridge.*                                            │
└──────────┬─────────────────────────────────────┬────────────┘
           │ JdbcTemplate / JDBC                  │ Spring Data MongoDB
           │ mysql-connector-j                    │ (optional)
           ▼                                      ▼
┌──────────────────────────┐          ┌──────────────────────┐
│  MySQL 8.0 (port 3306)   │          │  MongoDB (optional)   │
│  Database: foodbridge     │          │  Database: foodbridge  │
│  InnoDB storage engine    │          │  Collections: analytics│
│  10 normalized tables     │          │  Graceful fallback     │
└──────────────────────────┘          └──────────────────────┘
```

---

## 2. Component Architecture

### 2.1 Frontend Components

```
frontend/
├── index.html              # Landing page / public listing browser
├── login.html              # Authentication
├── register.html           # Registration (user + organization)
├── dashboard.html          # Role-adaptive dashboard (shared shell)
├── listings/
│   ├── browse.html         # Public listing browser with filters
│   ├── create.html         # Provider: create new listing (PROVIDER only)
│   └── detail.html         # Listing detail + request submission
├── requests/
│   ├── my-requests.html    # Recipient: view submitted requests
│   └── incoming.html       # Provider: view requests on their listings
├── allocations/
│   ├── my-allocations.html # Recipient: view allocations
│   └── manage.html         # Provider: approve/reject requests
├── collections/
│   └── my-collections.html # Track pickup confirmations
├── analytics/
│   └── dashboard.html      # Charts for provider/recipient analytics
├── admin/
│   ├── dashboard.html      # Platform overview (ADMIN only)
│   ├── users.html          # User management
│   └── audit.html          # Audit log viewer
├── css/
│   └── style.css           # Custom styles supplementing Bootstrap
└── js/
    ├── api.js              # Centralized fetch() wrapper with auth headers
    ├── auth.js             # Login/logout/session state management
    └── charts.js           # Chart.js initialization helpers
```

**Key frontend design decisions:**
- **No build step**: Pure HTML/JS — no webpack, npm, or transpilation needed. Files open directly in a browser.
- **Centralized API client** (`api.js`): All `fetch()` calls go through one module that attaches credentials, handles 401 redirects, and parses JSON.
- **Bootstrap 5**: Provides responsive grid, modals, alerts, and form components without custom CSS.
- **Chart.js**: Renders provider/recipient analytics charts from API data.

---

### 2.2 Backend Components

```
com.foodbridge/
├── FoodBridgeApplication.java          # Main class, @SpringBootApplication
├── config/
│   ├── SecurityConfig.java             # Spring Security: auth rules, CORS, session
│   ├── WebConfig.java                  # CORS configuration for frontend origin
│   └── MongoConfig.java                # Conditional MongoDB bean (graceful fallback)
├── controller/
│   ├── AuthController.java             # POST /api/v1/auth/**
│   ├── OrganizationController.java     # GET/PUT /api/v1/organizations/**
│   ├── FoodListingController.java      # /api/v1/listings/**
│   ├── FoodRequestController.java      # /api/v1/requests/**
│   ├── AllocationController.java       # /api/v1/allocations/**
│   ├── CollectionController.java       # /api/v1/collections/**
│   ├── NotificationController.java     # /api/v1/notifications/**
│   ├── AnalyticsController.java        # /api/v1/analytics/**
│   └── AdminController.java            # /api/v1/admin/**
├── service/
│   ├── UserService.java                # User registration, login, profile
│   ├── FoodListingService.java         # Listing lifecycle management
│   ├── FoodRequestService.java         # Request submission and management
│   ├── AllocationService.java          # Transactional allocation approval
│   ├── CollectionService.java          # Two-party pickup confirmation
│   ├── NotificationService.java        # In-app notification dispatch
│   ├── AnalyticsService.java           # SQL aggregate analytics
│   └── AdminService.java               # Admin operations
├── repository/
│   ├── UserRepository.java             # JdbcTemplate: users table
│   ├── OrganizationRepository.java     # JdbcTemplate: organizations table
│   ├── FoodListingRepository.java      # JdbcTemplate: food_listings table
│   ├── FoodRequestRepository.java      # JdbcTemplate: food_requests table
│   ├── AllocationRepository.java       # JdbcTemplate: allocations table
│   ├── CollectionRepository.java       # JdbcTemplate: collections table
│   └── NotificationRepository.java     # JdbcTemplate: notifications table
├── model/
│   ├── User.java                       # User domain object
│   ├── Organization.java               # Organization domain object
│   ├── FoodListing.java                # Listing domain object
│   ├── FoodRequest.java                # Request domain object
│   ├── Allocation.java                 # Allocation domain object
│   └── Collection.java                 # Collection domain object
├── dto/
│   ├── request/                        # Incoming API request bodies
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   ├── CreateListingRequest.java
│   │   └── ...
│   └── response/                       # Outgoing API response shapes
│       ├── ApiResponse.java            # Wrapper: {success, data, message}
│       ├── UserResponse.java
│       └── ...
├── exception/
│   ├── GlobalExceptionHandler.java     # @ControllerAdvice: maps exceptions to HTTP
│   ├── ResourceNotFoundException.java  # → 404
│   ├── AccessDeniedException.java      # → 403
│   └── InsufficientQuantityException.java # → 409 Conflict
└── security/
    └── FoodBridgeUserDetailsService.java  # Loads UserDetails from DB for Spring Security
```

---

## 3. Request/Response Flow — Typical API Call

**Example: Recipient browses active listings**

```
Browser (browse.html)
    │
    │  1. User opens page, JS calls:
    │     GET /api/v1/listings?status=ACTIVE&category=Produce
    │     Cookie: JSESSIONID=abc123
    │
    ▼
Spring Security Filter Chain
    │  2. Check if session abc123 is valid → YES
    │  3. Load SecurityContext for this session
    │  4. /api/v1/listings is permitted for any authenticated user → PASS
    │
    ▼
FoodListingController.getListings(params)
    │  5. Extract query parameters (status, category, page, size)
    │  6. Call FoodListingService.getActiveListings(params)
    │
    ▼
FoodListingService.getActiveListings(params)
    │  7. Build filter criteria
    │  8. Call FoodListingRepository.findAll(criteria)
    │
    ▼
FoodListingRepository.findAll(criteria)
    │  9. Build SQL dynamically:
    │     SELECT fl.*, o.name FROM food_listings fl
    │     JOIN organizations o ON fl.provider_organization_id = o.organization_id
    │     WHERE fl.status = ? AND fl.category = ?
    │  10. Execute via JdbcTemplate.query()
    │  11. Map ResultSet rows to List<FoodListing> via ListingRowMapper
    │
    ▼
FoodListingService (returns List<FoodListing>)
    │  12. Convert to List<ListingResponse> DTOs (hide internal fields)
    │
    ▼
FoodListingController
    │  13. Wrap in ApiResponse.success(data)
    │  14. Return ResponseEntity<ApiResponse> with HTTP 200
    │
    ▼
Spring MVC / Jackson
    │  15. Serialize ApiResponse to JSON
    │
    ▼
Browser
    │  16. JS receives JSON, renders listing cards on page
```

---

## 4. Allocation Transaction Flow

The most complex flow in the system — approving a food request and creating a confirmed allocation:

```
Provider clicks "Approve" on incoming request
    │
    │  POST /api/v1/allocations/approve/{requestId}
    │  Role: PROVIDER, owns the listing
    │
    ▼
AllocationController.approveRequest(requestId)
    │  1. Extract current user from SecurityContext
    │  2. Call AllocationService.approveAllocation(requestId, currentUser)
    │
    ▼
AllocationService.approveAllocation(requestId, approver)
    │  ┌─ BEGIN TRANSACTION ──────────────────────────────────────┐
    │  │                                                           │
    │  │  3. Fetch FoodRequest by requestId                        │
    │  │     → 404 if not found                                    │
    │  │     → 409 if status != 'PENDING'                          │
    │  │                                                           │
    │  │  4. Verify approver owns the listing                      │
    │  │     → 403 if not owner                                    │
    │  │                                                           │
    │  │  5. Conditional UPDATE on food_listings:                  │
    │  │     UPDATE food_listings                                   │
    │  │     SET available_quantity = available_quantity - :qty,   │
    │  │         status = (CASE WHEN remaining <= 0                 │
    │  │                        THEN 'FULLY_ALLOCATED'             │
    │  │                        ELSE 'PARTIALLY_ALLOCATED' END)    │
    │  │     WHERE listing_id = :id AND available_quantity >= :qty │
    │  │     → ROLLBACK + 409 if rowsAffected == 0                 │
    │  │                                                           │
    │  │  6. UPDATE food_requests SET status = 'APPROVED'          │
    │  │                                                           │
    │  │  7. INSERT INTO allocations (request_id, listing_id, ...) │
    │  │                                                           │
    │  │  8. INSERT INTO collections (allocation_id, ...)          │
    │  │                                                           │
    │  │  9. INSERT INTO notifications (recipient's user_id, ...)  │
    │  │                                                           │
    │  └─ COMMIT ────────────────────────────────────────────────┘
    │
    ▼
Response: 200 OK { allocationId, status, allocatedQuantity }
```

---

## 5. Authentication Flow

FoodBridge uses **server-side session authentication** (not JWT tokens):

```
LOGIN
─────
1. POST /api/v1/auth/login {email, password}
2. FoodBridgeUserDetailsService loads UserDetails from DB by email
3. Spring Security's DaoAuthenticationProvider compares BCrypt hash
4. On success: SecurityContextHolder populated with Authentication object
5. Spring creates HttpSession (JSESSIONID cookie sent to browser)
6. Response: 200 OK { user profile data }

SUBSEQUENT REQUESTS
───────────────────
1. Browser sends JSESSIONID cookie automatically
2. Spring Security's SessionManagementFilter looks up session
3. Restores SecurityContext from session store
4. Request proceeds to controller with authenticated user

LOGOUT
──────
1. POST /api/v1/auth/logout
2. HttpSession.invalidate() — destroys server-side session
3. SecurityContextHolder.clearContext()
4. Browser deletes JSESSIONID cookie
5. Response: 200 OK

SESSION SECURITY SETTINGS (application.properties)
────────────────────────────────────────────────────
server.servlet.session.timeout=30m
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.same-site=strict
```

---

## 6. MongoDB Integration and Graceful Fallback

```
application.properties
─────────────────────
mongodb.enabled=false  (default — MongoDB not required)
spring.autoconfigure.exclude=...MongoAutoConfiguration

MongoConfig.java
────────────────
@Configuration
@ConditionalOnProperty(name = "mongodb.enabled", havingValue = "true")
public class MongoConfig {
    @Bean public MongoClient mongoClient() { ... }
}

AnalyticsService.java (fallback pattern)
────────────────────────────────────────
public ProviderAnalytics getProviderAnalytics(int orgId) {
    // Always available: MySQL-computed analytics
    ProviderAnalytics analytics = mysqlAnalyticsQuery(orgId);

    // Optional enhancement: MongoDB aggregations
    if (mongoEnabled) {
        try {
            analytics.setExtendedMetrics(mongoRepo.getExtended(orgId));
        } catch (MongoException e) {
            log.warn("MongoDB unavailable, skipping extended metrics");
            // Return MySQL-only analytics — no exception propagated
        }
    }
    return analytics;
}
```

**What works without MongoDB:**
- ✅ Full listing/request/allocation/collection lifecycle
- ✅ Basic analytics (MySQL aggregate queries)
- ✅ Notifications, audit log, issues
- ✅ Admin panel

**What requires MongoDB (optional):**
- Extended analytics aggregation pipeline
- Flexible document metadata for listings

---

## 7. CORS Configuration

The frontend (`d:\FootBridge\frontend\`) runs from a different origin than the backend (`localhost:8080`). Spring Security's CORS configuration in `SecurityConfig.java` allows:

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*", "null"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true); // Required for session cookies
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
}
```

`allowCredentials(true)` is essential because the JSESSIONID cookie must be included in cross-origin requests.

---

## 8. Error Handling Architecture

All exceptions flow through `GlobalExceptionHandler.java` (`@ControllerAdvice`):

```
Exception Type                      → HTTP Status → Response Body
────────────────────────────────────────────────────────────────
ResourceNotFoundException           → 404         → {success:false, message:"..."}
AccessDeniedException               → 403         → {success:false, message:"..."}
InsufficientQuantityException       → 409         → {success:false, message:"..."}
MethodArgumentNotValidException     → 400         → {success:false, errors:[{field,msg}]}
AuthenticationException             → 401         → {success:false, message:"..."}
DataIntegrityViolationException     → 409         → {success:false, message:"Duplicate entry"}
Exception (catch-all)               → 500         → {success:false, message:"Internal error"}
```

All API responses use the same wrapper: `ApiResponse<T> { boolean success; T data; String message; }`.

---

## 9. Scheduled Tasks

| Task | Schedule | Implementation |
|------|----------|---------------|
| Expire overdue listings | Every hour | `@Scheduled(cron = "0 0 * * * *")` in `ListingExpiryTask.java` calls `CALL sp_expire_listings(NOW())` |
| Generate daily analytics snapshot | Daily at 2 AM | `@Scheduled(cron = "0 0 2 * * *")` in `AnalyticsSnapshotTask.java` |

---

## 10. Technology Selection Rationale

| Technology | Choice | Reason |
|-----------|--------|--------|
| Language | Java 21 | LTS release with virtual threads (Project Loom), records, pattern matching |
| Framework | Spring Boot 3.2 | Industry-standard, auto-configuration reduces boilerplate, mature ecosystem |
| DB access | JdbcTemplate | Explicit SQL gives full control over query optimization; avoids ORM magic |
| ORM | None (by design) | JPA/Hibernate would obscure the critical conditional UPDATE anti-double-allocation logic |
| Auth | Session-based | Simpler than JWT for a monolith; secure with `HttpOnly`/`SameSite` cookies |
| Frontend | HTML/JS/Bootstrap | No build toolchain; any text editor can modify; zero compilation step |
| Charts | Chart.js | Lightweight, CDN-delivered, simple API for bar/pie/line charts |
| MySQL | InnoDB engine | ACID transactions, row-level locking, FK constraints — all required for this domain |
| MongoDB | Optional | Document flexibility for analytics; graceful fallback preserves core functionality |
| Connection pool | HikariCP (default) | Fastest JVM connection pool; auto-configured by Spring Boot |

# FoodBridge — REST API Specification
## Base URL: `/api/v1`

All responses follow the unified response structure:
```json
{
  "success": true,
  "message": "Optional message",
  "data": { ... }
}
```

---

## 1. Authentication Endpoints (`/api/v1/auth`)

### `POST /api/v1/auth/register`
- **Access:** Public
- **Request Body:**
  ```json
  {
    "fullName": "Ravi Shankar",
    "email": "ravi@vitvellore.edu",
    "password": "Password@123",
    "phone": "9876543210",
    "role": "PROVIDER",
    "organizationName": "Sunrise Cafeteria VIT",
    "organizationType": "CAFETERIA",
    "description": "Student cafeteria surplus",
    "address": "VIT University Katpadi Road",
    "city": "Vellore",
    "state": "Tamil Nadu",
    "pincode": "632014"
  }
  ```
- **Response (201 Created):**
  ```json
  {
    "success": true,
    "message": "Registration successful",
    "data": {
      "userId": 2,
      "email": "ravi@vitvellore.edu",
      "fullName": "Ravi Shankar",
      "role": "PROVIDER",
      "organizationId": 1,
      "organizationName": "Sunrise Cafeteria VIT",
      "organizationStatus": "PENDING"
    }
  }
  ```

### `POST /api/v1/auth/login`
- **Access:** Public
- **Request Body:**
  ```json
  {
    "email": "ravi@vitvellore.edu",
    "password": "Provider@123"
  }
  ```
- **Response (200 OK):**
  Sets HTTP session cookie (`JSESSIONID`). Returns `AuthResponse`.

### `POST /api/v1/auth/logout`
- **Access:** Authenticated
- **Response (200 OK):** Invalidates session and clears security context.

### `GET /api/v1/auth/me`
- **Access:** Authenticated
- **Response (200 OK):** Returns profile of currently authenticated user.

---

## 2. Organization Endpoints (`/api/v1/organizations`)

### `GET /api/v1/organizations/me`
- **Access:** Authenticated
- **Response (200 OK):** Returns caller's organization details.

### `PUT /api/v1/organizations/me`
- **Access:** Authenticated
- **Request Body:** Organization profile updates.

### `GET /api/v1/organizations/{id}`
- **Access:** Authenticated
- **Response (200 OK):** Organization details by ID.

---

## 3. Food Listing Endpoints (`/api/v1/listings`)

### `GET /api/v1/listings`
- **Access:** Public
- **Query Params:** `category`, `city`, `page` (default 0), `size` (default 10)
- **Response (200 OK):** Paginated active listings (`PageResponse<FoodListing>`).

### `GET /api/v1/listings/{id}`
- **Access:** Public
- **Response (200 OK):** Full listing details including provider organization information.

### `POST /api/v1/listings`
- **Access:** Role `PROVIDER`
- **Request Body:**
  ```json
  {
    "foodName": "Veg Biryani",
    "description": "Vegetable biryani with raita",
    "category": "COOKED_MEAL",
    "totalQuantity": 50.00,
    "unit": "portions",
    "dietaryInfo": "VEGETARIAN",
    "allergenNotes": "None",
    "preparedAt": "2026-10-11T12:00:00",
    "expiresAt": "2026-10-11T20:00:00",
    "pickupDeadline": "2026-10-11T18:00:00",
    "pickupAddress": "VIT Cafeteria Main Block",
    "pickupCity": "Vellore"
  }
  ```
- **Response (201 Created):** Created `FoodListing`.

### `PUT /api/v1/listings/{id}`
- **Access:** Role `PROVIDER` (Owner only)
- **Response (200 OK):** Updated `FoodListing`.

### `DELETE /api/v1/listings/{id}`
- **Access:** Role `PROVIDER` (Owner only)
- **Response (200 OK):** Marks listing as `CANCELLED`.

### `GET /api/v1/provider/listings`
- **Access:** Role `PROVIDER`
- **Response (200 OK):** Provider's own food inventory across all statuses.

---

## 4. Food Request Endpoints (`/api/v1/requests`)

### `POST /api/v1/listings/{listingId}/requests`
- **Access:** Role `RECIPIENT` (Verified organization only)
- **Request Body:**
  ```json
  {
    "requestedQuantity": 20.00,
    "message": "Requesting 20 portions for evening distribution"
  }
  ```
- **Response (201 Created):** Created `FoodRequest` in `PENDING` status.

### `GET /api/v1/requests/me`
- **Access:** Role `RECIPIENT`
- **Response (200 OK):** Recipient's submitted requests.

### `GET /api/v1/provider/requests`
- **Access:** Role `PROVIDER`
- **Response (200 OK):** Incoming pending claims for provider's listings.

### `PATCH /api/v1/requests/{id}/cancel`
- **Access:** Role `RECIPIENT` (Owner only)
- **Response (200 OK):** Cancels `PENDING` request.

---

## 5. Allocation & Collection Endpoints

### `POST /api/v1/requests/{requestId}/approve`
- **Access:** Role `PROVIDER` (Listing owner only)
- **Execution:** Runs in `@Transactional(isolation = Isolation.SERIALIZABLE)`. Atomically decrements available quantity via conditional update.
- **Response (201 Created):** Created `Allocation` (`CONFIRMED`) and linked `CollectionRecord` (`SCHEDULED`).

### `GET /api/v1/allocations/me`
- **Access:** Role `RECIPIENT`
- **Response (200 OK):** Confirmed allocations for recipient.

### `POST /api/v1/allocations/{id}/cancel`
- **Access:** Authenticated (Recipient or Provider owner)
- **Execution:** Restores inventory to listing, cancels linked collection and request.

### `GET /api/v1/collections/me`
- **Access:** Authenticated
- **Response (200 OK):** Handover collection tasks for caller.

### `POST /api/v1/collections/{id}/confirm-provider`
- **Access:** Role `PROVIDER`
- **Response (200 OK):** Records provider handover timestamp.

### `POST /api/v1/collections/{id}/confirm-recipient`
- **Access:** Role `RECIPIENT`
- **Response (200 OK):** Records recipient receipt timestamp. If both confirmed, transitions status to `COLLECTED`.

---

## 6. Notifications (`/api/v1/notifications`)

- `GET /api/v1/notifications` — Paginated user notifications.
- `GET /api/v1/notifications/unread-count` — Count of unread alerts.
- `PATCH /api/v1/notifications/{id}/read` — Mark notification read.
- `POST /api/v1/notifications/mark-all-read` — Mark all read.

---

## 7. Analytics & Reports (`/api/v1/analytics`, `/api/v1/reports`)

- `GET /api/v1/reports/impact` — Public summary of platform-wide redistribution metrics.
- `GET /api/v1/analytics/provider` — Role `PROVIDER`: listings, claims, and collection stats.
- `GET /api/v1/analytics/recipient` — Role `RECIPIENT`: requests, claims, and collections.
- `GET /api/v1/analytics/categories` — Authenticated: category distribution dataset for charts.
- `GET /api/v1/analytics/monthly-collections` — Authenticated: monthly handover trends.
- `GET /api/v1/analytics/request-status` — Authenticated: request status distribution.

---

## 8. Administrative Endpoints (`/api/v1/admin`)

- `GET /api/v1/admin/analytics` — Role `ADMIN`: full platform metrics.
- `GET /api/v1/admin/users` — Role `ADMIN`: user accounts list.
- `PATCH /api/v1/admin/users/{id}/status` — Role `ADMIN`: suspend or activate user.
- `GET /api/v1/admin/organizations/pending` — Role `ADMIN`: pending verification queue.
- `PATCH /api/v1/admin/organizations/{id}/verification` — Role `ADMIN`: approve or reject organization.
- `GET /api/v1/admin/audit` — Role `ADMIN`: paginated immutable audit logs.
- `GET /api/v1/admin/issues` — Role `ADMIN`: reported operational issues.

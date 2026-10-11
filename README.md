# FoodBridge — From Surplus to Sustenance

> A full-stack food redistribution platform connecting providers with surplus food to verified organizations that can redistribute it to people in need.

**This is a university DBMS project** developed as part of the B.Tech Computer Science and Engineering (Data Science) curriculum at VIT Vellore. All statistics and impact data shown are **synthetic demonstration data** and do not represent actual operational results.

---

## Table of Contents

- [Problem Statement](#problem-statement)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Directory Structure](#directory-structure)
- [Prerequisites](#prerequisites)
- [Setup Guide](#setup-guide)
- [Demo Accounts](#demo-accounts)
- [API Overview](#api-overview)
- [Running Tests](#running-tests)
- [Known Limitations](#known-limitations)
- [Troubleshooting](#troubleshooting)

---

## Problem Statement

Restaurants, cafeterias, bakeries, and event organizers often have surplus food that could go to waste. At the same time, NGOs, shelters, and community kitchens may need food but lack a coordinated channel to discover available donations.

Manual coordination results in delayed communication, duplicated requests, poor visibility, and unreliable records.

**FoodBridge** provides a centralized platform where:
- Providers publish available surplus food listings
- Verified recipients discover and request suitable listings
- The platform coordinates requests, allocation, collection, and reporting
- Administrators verify organizations and monitor platform activity

---

## Features

| Module | Description |
|--------|-------------|
| **Authentication** | Registration, login, BCrypt passwords, session-based auth, role enforcement |
| **Organization Management** | Profiles for providers and recipients, admin verification workflow |
| **Food Listing Management** | Create, edit, cancel, search, filter, expiry handling |
| **Request Management** | Submit requests, track status, prevent duplicate active requests |
| **Allocation Engine** | Transaction-safe allocation preventing inventory overselling using conditional atomic UPDATE |
| **Pickup & Collection** | Two-party confirmation workflow (provider + recipient) |
| **Matching Engine** | Rule-based scoring: category, quantity, urgency, verification, feasibility |
| **Expiry Handling** | Scheduled task expires listings every 15 minutes; DB trigger for safety |
| **Notifications** | In-app notifications stored in MySQL, shown on dashboards |
| **Audit Logging** | Immutable audit trail of important actions |
| **Analytics** | SQL-backed dashboards with Chart.js visualizations |
| **Admin Dashboard** | User/org management, verification, issue tracking, platform reports |
| **MongoDB Integration** | Optional supplementary document store for flexible listing metadata |

---

## Technology Stack

### Frontend
- HTML5, CSS3, JavaScript (ES6+)
- Bootstrap 5 (CDN)
- Chart.js (CDN)
- Font Awesome 6 (CDN)
- Fetch API for backend communication

### Backend
- **Java 21** (OpenJDK Temurin 21.0.6)
- **Spring Boot 3.2.5**
- **Spring Web** — REST API (`/api/v1`)
- **Spring Security** — Session-based authentication, role-based authorization
- **Spring JDBC / JdbcTemplate** — All MySQL operations (no JPA/Hibernate)
- **Spring Validation** — Input validation
- **Spring Data MongoDB** — Optional supplementary document storage
- **Spring Scheduling** — Expiry background tasks
- **Lombok** — Boilerplate reduction
- **Maven 3.9.6** — Build tool

### Databases
- **MySQL 8.0** (primary) — All critical data: users, organizations, listings, requests, allocations, collections, notifications, audit logs
- **MongoDB 7.0** (optional) — Supplementary: listing metadata, organization profiles

### Testing
- JUnit 5, Mockito, Spring Security Test

---

## Directory Structure

```
FoodBridge/
├── README.md
├── .gitignore
├── .env.example
├── docker-compose.yml
├── docs/                          # Project documentation
│   ├── project-overview.md
│   ├── database-design.md         # ER, normalization, indexes, DBMS concepts
│   ├── system-architecture.md
│   ├── api-documentation.md
│   ├── test-cases.md
│   └── viva-questions.md          # 24 Q&A for project review
├── frontend/                      # Static HTML/CSS/JS application
│   ├── index.html                 # Public landing page
│   ├── login.html
│   ├── register.html
│   ├── dashboards/                # Role-specific dashboards
│   ├── pages/                     # Feature pages
│   ├── css/                       # Stylesheets
│   └── js/                        # JavaScript modules
├── backend/                       # Spring Boot Maven project
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/foodbridge/
│       │   ├── FoodBridgeApplication.java
│       │   ├── config/            # Security, CORS, MongoDB, Web
│       │   ├── controller/        # 11 REST controllers
│       │   ├── service/           # 9 service classes + scheduled task
│       │   ├── repository/        # 9 JdbcTemplate repositories
│       │   ├── mongodb/           # MongoDB documents, repos, service
│       │   ├── model/             # 10 domain models
│       │   ├── dto/               # Request/response DTOs
│       │   ├── enums/             # Status enums
│       │   ├── exception/         # Custom exceptions + global handler
│       │   └── util/              # Password, validation, impact utilities
│       └── test/                  # JUnit 5 + Mockito tests
├── database/
│   ├── mysql/                     # Canonical SQL scripts
│   │   ├── create_database.sql
│   │   ├── schema.sql             # All 10 tables with constraints
│   │   ├── indexes.sql
│   │   ├── views.sql              # 5 useful views
│   │   ├── triggers.sql           # 4 triggers
│   │   ├── procedures.sql         # 3 stored procedures
│   │   ├── seed_data.sql          # Synthetic demo data
│   │   └── analytical_queries.sql # Analytics SQL with EXPLAIN
│   └── mongodb/
│       ├── collection-design.md
│       ├── sample-documents.json
│       └── indexes.js
└── scripts/
    └── run-local.md               # Full setup guide
```

---

## Prerequisites

| Tool | Version | Path |
|------|---------|------|
| Java (JDK) | 21 | `C:\Program Files\BlueJ\jdk` |
| Maven | 3.9.6 | `D:\apache-maven-3.9.6` |
| MySQL | 8.0.46 | `C:\Program Files\MySQL\MySQL Server 8.0` |
| MongoDB | 7.0 (optional) | Required only for supplementary features |

---

## Setup Guide

### 1. Set Environment (PowerShell)

```powershell
$env:JAVA_HOME  = "C:\Program Files\BlueJ\jdk"
$env:PATH       = "D:\apache-maven-3.9.6\bin;$env:PATH"
$env:DB_PASSWORD = "your_mysql_root_password"
```

### 2. Create MySQL Database

```powershell
$mysql = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"

& $mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS foodbridge CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
& $mysql -u root -p foodbridge < "D:\FootBridge\database\mysql\schema.sql"
& $mysql -u root -p foodbridge < "D:\FootBridge\database\mysql\indexes.sql"
& $mysql -u root -p foodbridge < "D:\FootBridge\database\mysql\views.sql"
& $mysql -u root -p foodbridge < "D:\FootBridge\database\mysql\triggers.sql"
& $mysql -u root -p foodbridge < "D:\FootBridge\database\mysql\procedures.sql"
& $mysql -u root -p foodbridge < "D:\FootBridge\database\mysql\seed_data.sql"
```

### 3. Build and Run Backend

```powershell
cd D:\FootBridge\backend

$env:DB_USERNAME  = "root"
$env:DB_PASSWORD  = "your_password"
$env:MONGO_ENABLED = "false"

mvn clean package -DskipTests
& "C:\Program Files\BlueJ\jdk\bin\java.exe" -jar target\foodbridge-1.0.0.jar
```

Backend starts at: **http://localhost:8080**

### 4. Open Frontend

Open `D:\FootBridge\frontend\index.html` directly in a browser.

> The frontend communicates with the backend at `http://localhost:8080/api/v1`.

---

## Demo Accounts

> ⚠️ **These are synthetic demo credentials for local demonstration only. Do not use in production.**

| Role | Email | Password | Organization |
|------|-------|----------|-------------|
| Admin | `admin@foodbridge.local` | `Admin@123` | — |
| Provider | `ravi@vitvellore.edu` | `Provider@123` | Sunrise Cafeteria VIT |
| Provider | `priya@bakersdozen.in` | `Provider@123` | The Baker's Dozen |
| Provider | `karthik@spiceroute.in` | `Provider@123` | Spice Route Restaurant |
| Recipient | `meena@vellorехоpe.org` | `Recipient@123` | Vellore Hope Foundation |
| Recipient | `arjun@kitchen.org` | `Recipient@123` | Aadhara Community Kitchen |
| Recipient | `lakshmi@childrenfirst.in` | `Recipient@123` | Children First Shelter |

---

## API Overview

Base URL: `http://localhost:8080/api/v1`

| Method | Path | Role | Description |
|--------|------|------|-------------|
| `POST` | `/auth/register` | Public | Register new user + organization |
| `POST` | `/auth/login` | Public | Login, creates session |
| `POST` | `/auth/logout` | Auth | Invalidate session |
| `GET` | `/auth/me` | Auth | Current user info |
| `GET` | `/listings` | Public | Browse active listings (filter: category, city) |
| `POST` | `/listings` | Provider | Create food listing |
| `PUT` | `/listings/{id}` | Provider | Edit own listing |
| `DELETE` | `/listings/{id}` | Provider | Cancel own listing |
| `POST` | `/listings/{id}/requests` | Recipient | Request food |
| `GET` | `/requests/me` | Recipient | My request history |
| `GET` | `/provider/requests` | Provider | Incoming requests |
| `POST` | `/requests/{id}/approve` | Provider | Approve request (creates allocation) |
| `GET` | `/allocations/me` | Recipient | My confirmed allocations |
| `POST` | `/allocations/{id}/cancel` | Auth | Cancel confirmed allocation |
| `GET` | `/collections/me` | Auth | My collection records |
| `POST` | `/collections/{id}/confirm-provider` | Provider | Confirm handover |
| `POST` | `/collections/{id}/confirm-recipient` | Recipient | Confirm receipt |
| `GET` | `/notifications` | Auth | My notifications |
| `GET` | `/analytics/provider` | Provider | Provider dashboard stats |
| `GET` | `/analytics/recipient` | Recipient | Recipient dashboard stats |
| `GET` | `/reports/impact` | Public | Platform impact summary |
| `GET` | `/admin/analytics` | Admin | Full platform analytics |
| `GET` | `/admin/organizations/pending` | Admin | Pending verifications |
| `PATCH` | `/admin/organizations/{id}/verification` | Admin | Approve/reject org |
| `GET` | `/admin/users` | Admin | All users |
| `PATCH` | `/admin/users/{id}/status` | Admin | Suspend/activate user |

See `docs/api-documentation.md` for complete documentation.

---

## Running Tests

```powershell
cd D:\FootBridge\backend
$env:JAVA_HOME = "C:\Program Files\BlueJ\jdk"
$env:PATH = "D:\apache-maven-3.9.6\bin;$env:PATH"

# Run all unit tests (Mockito, no DB required)
mvn test

# Run specific test class
mvn test -Dtest=AllocationServiceTest
mvn test -Dtest=FoodListingServiceTest
```

Test classes:
- `AllocationServiceTest` — Allocation engine, concurrent access, inventory safety
- `FoodListingServiceTest` — Listing creation, validation, authorization
- `AuthServiceTest` — Registration, login, role validation
- `MatchingServiceTest` — Rule-based matching and scoring
- `AnalyticsServiceTest` — Metric calculation accuracy

---

## Known Limitations

1. **No email/SMS notifications** — All notifications are in-app only (stored in MySQL)
2. **No real geolocation matching** — Collection feasibility score is fixed; no map integration
3. **Single-user organizations** — Each organization has one owner user in this version
4. **No image uploads** — Listing image paths stored in MongoDB metadata only; no file server
5. **No real-time updates** — Frontend polls via page reload; no WebSockets
6. **MongoDB is optional** — Core workflows are 100% MySQL-backed; MongoDB adds supplementary metadata only
7. **Demo data only** — The seed data is entirely synthetic and does not represent real food redistribution
8. **Food safety** — The platform coordinates redistribution logistics only; it does not certify food safety

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| `mvn` not recognized | `$env:PATH = "D:\apache-maven-3.9.6\bin;$env:PATH"` |
| `java` version wrong | `$env:JAVA_HOME = "C:\Program Files\BlueJ\jdk"` |
| DB connection refused | Check MySQL is running: `Get-Service MySQL80` |
| 401 Unauthorized | Call `POST /api/v1/auth/login` first; credentials sent in session cookie |
| MongoDB errors | Set `MONGO_ENABLED=false` — MongoDB is fully optional |
| CORS errors in browser | Ensure backend is running on port 8080; check `CorsConfig.java` |
| Seed data errors | Run `schema.sql` first to create tables, then `seed_data.sql` |

---

## DBMS Concepts Demonstrated

| Concept | Implementation |
|---------|---------------|
| Normalization (1NF, 2NF, 3NF) | All 10 tables, documented in `docs/database-design.md` |
| Primary & Foreign Keys | All tables with ON DELETE/UPDATE policies |
| Transactions (ACID) | `AllocationService.approve()` with `@Transactional(SERIALIZABLE)` |
| Concurrency Control | Conditional atomic `UPDATE ... WHERE available_quantity >= ?` |
| Views | 5 views: active listings, collection summary, provider/recipient activity |
| Triggers | 4 triggers: audit on verification change, quantity validation, auto-status |
| Stored Procedures | 3 procedures: impact summary, batch expiry, provider stats |
| Indexes | 20+ indexes covering all search and join patterns |
| SQL Aggregations | Analytics queries with COUNT, SUM, GROUP BY, HAVING, subqueries |
| JdbcTemplate (JDBC) | All 9 repository classes use parameterized JdbcTemplate queries |
| MongoDB | Supplementary document store with graceful fallback |
| EXPLAIN | `analytical_queries.sql` includes EXPLAIN on active listing search |

# FoodBridge — Local Development Setup Guide

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Java | 21 (OpenJDK) | Available at `C:\Program Files\BlueJ\jdk` |
| Maven | 3.9.6 | Available at `D:\apache-maven-3.9.6` |
| MySQL | 8.0 | Running on port 3306 |
| MongoDB | 7.0 (optional) | Required only for supplementary features |

---

## Step 1: Environment Setup

Open PowerShell and set environment for this session:

```powershell
$env:JAVA_HOME = "C:\Program Files\BlueJ\jdk"
$env:PATH = "D:\apache-maven-3.9.6\bin;$env:PATH"
$env:DB_PASSWORD = "your_mysql_password_here"
```

Or create a `.env` file (not committed to git) and set these as system environment variables.

---

## Step 2: MySQL Database Setup

Run scripts in this order:

```powershell
$mysql = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"

# 1. Create database
& $mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS foodbridge CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2. Schema
& $mysql -u root -p foodbridge < "D:\FootBridge\database\mysql\schema.sql"

# 3. Indexes
& $mysql -u root -p foodbridge < "D:\FootBridge\database\mysql\indexes.sql"

# 4. Views
& $mysql -u root -p foodbridge < "D:\FootBridge\database\mysql\views.sql"

# 5. Triggers
& $mysql -u root -p foodbridge < "D:\FootBridge\database\mysql\triggers.sql"

# 6. Stored Procedures
& $mysql -u root -p foodbridge < "D:\FootBridge\database\mysql\procedures.sql"

# 7. Seed Data (demonstration data only)
& $mysql -u root -p foodbridge < "D:\FootBridge\database\mysql\seed_data.sql"
```

---

## Step 3: Configure Backend

The backend reads configuration from environment variables (or defaults):

```powershell
$env:DB_HOST      = "localhost"
$env:DB_PORT      = "3306"
$env:DB_NAME      = "foodbridge"
$env:DB_USERNAME  = "root"
$env:DB_PASSWORD  = "your_mysql_password"
$env:MONGO_ENABLED = "false"   # Set to "true" only if MongoDB is installed
```

---

## Step 4: Build and Run Backend

```powershell
cd D:\FootBridge\backend
$env:JAVA_HOME = "C:\Program Files\BlueJ\jdk"
$env:PATH = "D:\apache-maven-3.9.6\bin;$env:PATH"

# Build
mvn clean package -DskipTests

# Run
& "C:\Program Files\BlueJ\jdk\bin\java.exe" -jar target\foodbridge-1.0.0.jar
```

The backend will start on: http://localhost:8080

---

## Step 5: Access Frontend

Open `D:\FootBridge\frontend\index.html` in your browser.

Or, if you want the frontend served by the backend (optional):
- Copy everything in `frontend/` to `backend/src/main/resources/static/`
- Then access: http://localhost:8080/index.html

---

## Step 6: Demo Accounts

> ⚠️ These are synthetic demo credentials for the local demonstration only.

| Role | Email | Password | Organization |
|------|-------|----------|-------------|
| Admin | admin@foodbridge.local | Admin@123 | — |
| Provider | ravi@vitvellore.edu | Provider@123 | Sunrise Cafeteria VIT |
| Provider | priya@bakersdozen.in | Provider@123 | The Baker's Dozen |
| Provider | karthik@spiceroute.in | Provider@123 | Spice Route Restaurant |
| Recipient | meena@vellorехоpe.org | Recipient@123 | Vellore Hope Foundation |
| Recipient | arjun@kitchen.org | Recipient@123 | Aadhara Community Kitchen |
| Recipient | lakshmi@childrenfirst.in | Recipient@123 | Children First Shelter |

---

## Step 7: Run Tests

```powershell
cd D:\FootBridge\backend
$env:JAVA_HOME = "C:\Program Files\BlueJ\jdk"
$env:PATH = "D:\apache-maven-3.9.6\bin;$env:PATH"

# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=AllocationServiceTest
```

---

## API Quick Reference

Base URL: `http://localhost:8080/api/v1`

```
POST /auth/login           { "email": "...", "password": "..." }
POST /auth/register        { registration body }
GET  /listings             ?category=...&city=...&page=0&size=10
GET  /listings/{id}
POST /listings             (PROVIDER) create listing
POST /listings/{id}/requests (RECIPIENT) request food
POST /requests/{id}/approve (PROVIDER) approve request
GET  /allocations/me       (RECIPIENT) my allocations
GET  /collections/me       my collections
GET  /analytics/provider   (PROVIDER) my stats
GET  /admin/analytics      (ADMIN) platform stats
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `mvn` not found | Set `$env:PATH = "D:\apache-maven-3.9.6\bin;$env:PATH"` |
| `java` not found | Set `$env:JAVA_HOME = "C:\Program Files\BlueJ\jdk"` |
| DB connection failed | Verify MySQL is running: `Get-Service MySQL80` |
| 401 on API calls | Login first via POST /auth/login |
| MongoDB error | Set `MONGO_ENABLED=false` — MongoDB is optional |

---

## MongoDB (Optional)

If you want supplementary document features:

1. Install MongoDB Community 7.0
2. Start mongod service
3. Run: `mongosh foodbridge_meta < D:\FootBridge\database\mongodb\indexes.js`
4. Set `$env:MONGO_ENABLED = "true"` before starting backend

# Package Delivery System

A shipment management and tracking system built with **Spring Boot (Java 21)** on the
backend, **Angular 20** on the frontend, and **MySQL 8** for storage. It supports manual
and bulk (CSV) shipment entry, full status-history tracking, role-based access, filtering,
and an infinite-scroll list backed by keyset pagination.

## Features

- **Roles:** `user`, `courier`, `admin` (registration + BCrypt login).
  - **User:** creates shipments manually and views only their own.
  - **Courier:** views all shipments that are not yet delivered and changes their status.
  - **Admin:** bulk-imports CSV files and views all shipments.
- **Status lifecycle:** `CREATED → IN_TRANSIT → DELIVERED / CANCELLED`.
- **History:** every status change is logged (status, timestamp, note) and viewable per shipment.
- **Bulk CSV import:** asynchronous, multi-threaded batch inserts (handles ~1M rows).
- **Filtering:** by recipient username, status, and creation date.
- **Smart scroll:** keyset (cursor) pagination + `IntersectionObserver` infinite scroll.

## Project structure

```
.
├── backend/            Spring Boot REST API (plain JDBC + HikariCP)
├── frontend/           Angular 20 SPA (standalone components, signals)
├── init_script.sql     MySQL schema + status-history trigger
└── docker-compose.yml  Runs db + backend + frontend
```

## Running with Docker Compose (recommended)

Brings up the database, backend, and frontend with a single command:

```bash
docker compose up --build
```

- Frontend: http://localhost:4200
- Backend API: http://localhost:8080
- MySQL: localhost:3306

The database schema (tables + history trigger) is created automatically from
`init_script.sql`. To wipe data and start fresh:

```bash
docker compose down -v
```

### Seeding demo users

Users are created via the registration API. With the stack running:

```powershell
./seed-users.ps1
```

This creates:

| Role    | Username  | Password      |
|---------|-----------|---------------|
| Admin   | `admin`   | `admin123`    |
| Courier | `courier` | `courier123`  |
| User    | `user1`   | `password123` |

(`user2`–`user10` are also created with `password123`.)

## Environment variables

The backend reads its database configuration from environment variables (defaults shown):

| Variable      | Default          | Description              |
|---------------|------------------|--------------------------|
| `DB_HOST`     | `localhost`      | MySQL host               |
| `DB_PORT`     | `3306`           | MySQL port               |
| `DB_NAME`     | `package_system` | Database name            |
| `DB_USERNAME` | `root`           | Database user            |
| `DB_PASSWORD` | `password123`    | Database password        |

In `docker-compose.yml` these are set so the backend points at the `db` service.

## Running locally (without Docker)

Requires JDK 21, Node 20+, and a running MySQL with the schema from `init_script.sql`.

```bash
# Backend
cd backend
./mvnw spring-boot:run

# Frontend (in another terminal)
cd frontend
npm install
npm start        # serves on http://localhost:4200
```

## REST API overview

| Method | Endpoint                       | Description                                   |
|--------|--------------------------------|-----------------------------------------------|
| POST   | `/auth/register`               | Register a new user                           |
| POST   | `/auth/login`                  | Login, returns the user                       |
| POST   | `/shipments/create`            | Create a shipment (user/admin)                |
| POST   | `/shipments/update-status`     | Change a shipment status (courier/admin)      |
| GET    | `/shipments/search`            | List shipments (keyset paginated, filterable) |
| GET    | `/shipments/{id}/history`      | Status history for a shipment                 |
| POST   | `/shipments/import`            | Start an async CSV import (admin)             |
| GET    | `/shipments/import/{jobId}`    | Poll import job progress                      |

### CSV import format

```
tracking_number,description,current_status,customer_username,created_at
TRACK000000001,Package number 1,CREATED,user1,2025-01-01 00:00:01
```

`current_status` defaults to `CREATED` if blank; `created_at` is optional.

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
- **Auth:** JWT (HS256, 30 min expiry) issued on login and sent as a `Bearer` token; the role and username are read from the token, not from request parameters.
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
| `JWT_SECRET`  | (dev default)    | Secret used to sign JWTs |

In `docker-compose.yml` these are set so the backend points at the `db` service.
The defaults live in `.env` (read automatically by docker-compose).

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
| POST   | `/auth/register`               | Register a new user, returns a JWT          |
| POST   | `/auth/login`                  | Login, returns the user + JWT token         |
| POST   | `/shipments/create`            | Create a shipment (user/admin)                |
| POST   | `/shipments/update-status`     | Change a shipment status (courier/admin)      |
| GET    | `/shipments/search`            | List shipments (keyset paginated, filterable) |
| GET    | `/shipments/{id}/history`      | Status history for a shipment                 |
| POST   | `/shipments/import`            | Start an async CSV import (admin)             |
| GET    | `/shipments/import/{jobId}`    | Poll import job progress                      |

All `/shipments/**` endpoints require the `Authorization: Bearer <token>` header;
`/auth/**` is public.

### CSV import format

```
tracking_number,description,current_status,customer_username,created_at
TRACK000000001,Package number 1,CREATED,user1,2025-01-01 00:00:01
```

`current_status` defaults to `CREATED` if blank; `created_at` is optional.

## How it works

### Bulk CSV import (batch insert)

- The import is **asynchronous**: `POST /shipments/import` saves the file, starts a
  background thread, and immediately returns a `jobId`. Progress is polled via
  `GET /shipments/import/{jobId}` (`imported` / `failed` counters).
- The file is streamed and parsed row by row. Each row is validated (tracking number
  present, customer username exists) and accumulated into **batches of 500**.
- Batches are inserted **in parallel** by a pool of insert threads. Each batch runs in a
  single transaction (one commit per batch) using JDBC `addBatch()` / `executeBatch()`,
  with `rewriteBatchedStatements=true` on the JDBC URL so the driver collapses them into
  one multi-row `INSERT` for speed.
- For every inserted shipment the initial `CREATED` history row is written in the **same
  transaction** (using the generated ids), so a shipment and its history are always
  consistent.
- **Resilient fallback:** if a bulk batch fails (e.g. a duplicate tracking number), it is
  rolled back and retried **row by row** — the valid rows still get inserted, only the bad
  ones are skipped and counted as `failed`.
- Result: ~1M rows import in well under a minute.

### Keyset pagination + infinite scroll

- `GET /shipments/search` does **not** use `OFFSET` (which gets slower the deeper you
  page). It uses **keyset (cursor) pagination**:
  `WHERE id < :cursor ORDER BY id DESC LIMIT :limit`.
- The first request omits the cursor (newest first). Each response's last `id` becomes the
  `cursor` for the next page, so every page is a fast **primary-key range scan** —
  effectively constant time even at a million rows.
- Filter columns (`customer_username`, `current_status`, `created_at`) are indexed
  (see `init_script.sql`) to keep filtered searches fast.
- On the frontend, an `IntersectionObserver` watches a sentinel element at the bottom of
  the list and loads the next page automatically as the user scrolls.

### Status-history trigger

- The `status_logs` table holds the full history of every shipment (status, `changed_at`,
  note).
- A MySQL trigger **`after_shipment_update`** (in `init_script.sql`) automatically inserts
  a history row whenever a shipment's `current_status` changes, with a note like
  `Status changed from CREATED to IN_TRANSIT.` — so every manual status change is logged
  with no extra application code.
- The initial `CREATED` history row is written by the application at insert time (the import
  does this in bulk for performance).
- History is exposed via `GET /shipments/{id}/history` and the **View** button in the UI.

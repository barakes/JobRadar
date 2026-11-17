# JobRadar

[![CI](https://github.com/barakes/JobRadar/actions/workflows/ci.yml/badge.svg)](https://github.com/barakes/JobRadar/actions/workflows/ci.yml)

A backend service that aggregates job postings from public applicant-tracking systems,
keeps them in sync on a schedule, and offers full-text search with country and location filters.

**Live API docs:** https://jobradar-3wmg.onrender.com/swagger-ui.html
*(Free hosting: the first request after a period of inactivity can take about 2 minutes while the server wakes up.)*

Try it: open the docs, expand `GET /api/jobs`, click **Try it out**, and search for `engineer` with country `israel`.

---

## Features

- **Multiple job sources** – Greenhouse and Lever public job-board APIs. Each source is a single class; adding one requires no changes to the sync logic.
- **Scheduled sync** – new jobs are created, existing ones updated, and jobs that disappear from the source are marked as closed (and reopened if they come back).
- **Deduplication** – enforced both in code and by a database unique constraint.
- **Full-text search** – PostgreSQL `tsvector` with a GIN index, supporting web-search syntax (`backend -senior`, `"exact phrase"`).
- **Country detection** – companies write locations differently ("Tel Aviv", "Israel", "Petah Tikva"); each job gets a normalized country, so one filter finds them all.
- **Pagination** with a capped page size.
- **Admin API key** protecting every endpoint that changes data; search stays public.
- **Interactive API documentation** with OpenAPI / Swagger UI.

## Tech stack

| Area | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.1 (Web MVC, Data JPA, Validation) |
| Database | PostgreSQL 18, Flyway migrations |
| HTTP client | Spring `RestClient` with timeouts |
| Docs | springdoc-openapi (Swagger UI) |
| Tests | JUnit 6, AssertJ, Testcontainers |
| CI/CD | GitHub Actions |
| Hosting | Docker on Render, database on Neon |

## Architecture

```mermaid
flowchart LR
    GH[Greenhouse API] --> C1[GreenhouseClient]
    LV[Lever API] --> C2[LeverClient]
    C1 --> S[JobSyncService]
    C2 --> S
    SCH[Scheduler / GitHub Actions cron] --> S
    S --> DB[(PostgreSQL)]
    DB --> API[Search API]
    API --> U[Clients]
```

## Design decisions

- **Network calls happen outside database transactions.** Fetching from an external API can take seconds; holding a database connection during that time could exhaust the connection pool. Jobs are fetched first, then saved in a short transaction.
- **Strategy pattern for sources.** All `JobSourceClient` implementations are injected as a list and mapped by source type, so a new source is just a new class (open/closed principle).
- **Failure isolation.** When syncing all companies, one failing source is logged and skipped; the others continue.
- **No overlapping syncs.** An `AtomicBoolean` lock ensures a scheduled sync and a manual one never run at the same time.
- **Encapsulated entities.** No setters; state changes go through methods like `refresh()` and `markClosed()`, and the country is always derived from the location.
- **DTOs instead of entities** in API responses, and a projection query that fetches only the columns a search result needs (no N+1 queries).
- **Constant-time key comparison** (`MessageDigest.isEqual`) to avoid timing attacks; secrets come only from environment variables.
- **Waking a sleeping free-tier server.** A GitHub Actions cron job calls the sync endpoint every 6 hours, which both wakes the server and triggers a sync.

## API overview

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/jobs?q=&country=&location=&page=&size=` | – | Search open jobs |
| GET | `/api/companies` | – | List tracked companies |
| POST | `/api/companies` | Admin key | Track a new company |
| POST | `/api/companies/{id}/sync` | Admin key | Sync one company |
| POST | `/api/sync` | Admin key | Sync all active companies |

Admin endpoints require the `X-Admin-Key` header.

## Running locally

Requirements: Java 25 and Docker.

```bash
# Start PostgreSQL
docker compose up -d

# Run the application
./mvnw spring-boot:run
```

Then open http://localhost:8080/swagger-ui.html.
Locally, the admin key is `local-dev-key`.

## Tests

```bash
./mvnw test
```

36 tests, including integration tests that run against a real PostgreSQL instance started by Testcontainers
(sync behavior, deduplication, closing and reopening jobs, full-text search, filters and pagination).

## Roadmap

- Saved searches with Telegram alerts for new matching jobs
- Stemming in full-text search (e.g. `engineer` matching `engineering`)
- More sources (e.g. Comeet, popular with Israeli companies)
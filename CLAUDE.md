# CLAUDE.md

Project guidance for AI agents. opencode reaches this file through the pointer
in `AGENTS.md`; Claude Code reads it directly.

## What this is

The E-Commerce-Hub of Gruppe 02 (Projektseminar Programmierung, HS Aalen):
a supplement shop for Hyrox athletes. Angular SPA + Spring Boot REST API + MariaDB,
wired together by `docker-compose`. The domain language (UI texts, docs, error
messages) is **German**; code identifiers are English.

| | |
| --- | --- |
| Frontend | Angular 22 + Angular Material → static files, served by nginx:80 |
| Backend | Spring Boot 4.1.1, Java 21, REST under `/api` |
| Database | MariaDB 11.4 (container `db`, volume `db-data`), see ADR-0001 |
| Tests | JUnit + MockMvc against H2 (backend), Vitest via `ng test` (frontend) |

## Architecture

```
browser ──▶ nginx :80 ──▶ index.html + hashed JS/CSS + /images/*.svg
                │
                └──▶ /api/* ──▶ backend:8080 ──▶ db:3306 (Compose network only)
```

- The frontend always requests **relative** paths (`/api/products`). Never hard-code
  a host, port or absolute URL into the Angular app — that keeps CORS out of the project.
- `backend` and `db` are **not published** to the host. That is deliberate.
- Startup order is enforced by healthchecks: `db` (healthcheck.sh) → `backend`
  (`/api/health`) → `frontend`. Do not add sleeps or restart loops.
- Schema is created by Hibernate (`ddl-auto=update`) in Sprint 1. Sample data is
  inserted by `SampleDataLoader` only when there are no categories yet.

## Domain rules (see docs/anforderungen/)

- One product per flavour in Sprint 1 ("Whey Protein Schoko"); variants come later.
- Customers see `stockStatus` (`IN_STOCK` > 5, `LOW_STOCK` 1–5, `SOLD_OUT` 0), computed
  only in `StockStatus.java`. The admin page shows the exact `stock`.
- Product list = infinite scroll, 12 per page (ADR-0002). Search (`q`) and category
  (`kategorie`) live in the URL.
- A category with products cannot be deleted (409). Category names are unique, case-insensitive.
- Validation is binding in the backend; the frontend mirrors the rules with the same
  German messages (ADR-0003). Errors are Problem Details with an `errors` list per field.

## Conventions

**Backend** (`de.hsaa.shop`, packages by feature: `product`, `category`, `common`, `sampledata`)

- Entities are plain JPA classes; API in/out are Java `record`s (`ProductRequest`,
  `ProductPatch`, `ProductResponse`, …). No Lombok. Never return an entity from a controller.
- Controllers stay thin; business rules live in `@Service` classes with `@Transactional`.
- Throw `NotFoundException` (404), `ConflictException` (409) or `InvalidFieldException`
  (400 + field); `GlobalExceptionHandler` turns them into Problem Details.
- User-facing messages are German, complete sentences, no technical details.

**Frontend**

- Standalone components, signals for state, `inject()` for DI.
- All HTTP goes through services in `src/app/api/`; components never use `HttpClient` directly.
- Error texts come from `shared/errors.ts` (`userMessage`, `fieldErrors`); price/stock
  parsing and the stock labels from `shared/format.ts`.
- German locale is registered in `app.config.ts`; use the `currency` pipe for prices.
- Formatting: Prettier (`frontend/.prettierrc`). Run `npx prettier --write src` in `frontend/`.
- Tests use Vitest through `ng test` (jsdom, no browser needed).

**Docker**

- Images are built the production way, no hot reload in containers.
- Reset data: `docker-compose down -v`.

## Gotchas

- **Corporate TLS interception.** Maven Central may arrive re-signed by a proxy CA and
  the build fails with `PKIX path building failed`. Put the root CA into
  `backend/docker/certs/` as `.pem` (gitignored, never commit it).
- **Tests run on H2, production on MariaDB.** SQL that only works in one of them will
  pass the tests and fail at runtime (or vice versa). Stick to JPA/Spring Data queries.
- **Dev proxy.** `ng serve` needs `proxy.conf.json`
  (`{ "/api": { "target": "http://localhost:8080", "secure": false } }`).
- **nginx config is load-bearing.** `frontend/nginx.conf` hosts the SPA *and* proxies `/api/`.

## Architecture Decision Records

Significant choices are recorded as ADRs in `docs/adr/` (MADR 4.0, German), file name
`NNNN-kurztitel.md`. Use the project skill ("ein ADR erstellen"). ADRs are immutable once
`angenommen` — supersede with a new record instead of editing an old one. Commit ADRs together
with the code they describe.

Requirements (user stories with acceptance criteria) live in `docs/anforderungen/`
(skill `anforderung`).

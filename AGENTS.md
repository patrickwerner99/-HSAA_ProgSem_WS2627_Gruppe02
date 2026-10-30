# AGENTS.md

Project instructions for AI agents. opencode loads this file automatically.

## External file loading

**CRITICAL:** when you encounter a file reference such as `@CLAUDE.md`,
`@docs/...` or `@rules/...` anywhere in this repository, use the Read tool to
load it on a need-to-know basis.

Read **`CLAUDE.md`** early in the session — it holds the architecture, the
domain rules, the conventions and the operational gotchas that are not obvious from the code.

## Build and run

| Task | Command |
| --- | --- |
| Full stack | `docker compose up -d --build` → <http://localhost> |
| Backend on host | `cd backend && ./mvnw spring-boot:run` → <http://localhost:8080> (needs a reachable MariaDB) |
| Frontend on host | `cd frontend && npm start` → <http://localhost:4200> |
| Follow logs | `docker-compose logs -f` |
| Stop | `docker compose down` |
| Reset data | `docker compose down -v` |

## Verify before finishing

```bash
cd backend  && ./mvnw test                    # JUnit + MockMvc against H2, needs JDK 21
cd frontend && npm install && npx ng test     # vitest via `ng test`
```

`docker compose up --build` already runs both suites — treat a successful
`docker compose up -d --build` plus a green `docker-compose ps` as the final
check for a change that touches any container.

No linter is configured. `frontend/.prettierrc` exists; format with
`cd frontend && npx prettier --write src` when touching frontend files.

## Where things live

- `backend/src/main/java/de/hsaa/shop/product/` — products: entity, repository, service, controller, DTOs, `StockStatus`
- `backend/src/main/java/de/hsaa/shop/category/` — categories
- `backend/src/main/java/de/hsaa/shop/common/` — `GlobalExceptionHandler` (Problem Details), exceptions, health
- `backend/src/main/java/de/hsaa/shop/sampledata/` — sample data on first start
- `frontend/src/app/api/` — HTTP services and response types
- `frontend/src/app/pages/` — `product-list`, `product-detail`, `admin`, `not-found`
- `frontend/src/app/shared/` — error messages, formatting, fallback image, dialogs
- `frontend/public/images/` — product images (SVG) and placeholder
- `frontend/nginx.conf` — static hosting **and** the `/api` reverse proxy
- `docker-compose.yml` — service wiring; `db` and `backend` are deliberately not published
- `.opencode/skills/adr/` — ADR skill; it writes records into `docs/adr/`
- `docs/adr/` — Architecture Decision Records
- `.opencode/skills/anforderung/` — Skill `anforderung`; legt User Stories mit Akzeptanzkriterien an
- `docs/anforderungen/` — Anforderungen, eine Datei pro User Story

Architecture details, domain rules, conventions and gotchas: see **`CLAUDE.md`**.

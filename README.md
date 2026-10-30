# Hyrox-Shop – E-Commerce-Hub (Gruppe 02)

Projektseminar Programmierung, Hochschule Aalen, WS 26/27.
Ein Online-Shop für Sport-Supplements, spezialisiert auf Hyrox: Kreatin, Protein, Aminosäuren
und unser eigenes Hyrox-Supplement, jeweils in mehreren Sorten.

- **Frontend:** Angular 22 mit Angular Material, als statische Dateien von nginx ausgeliefert
- **Backend:** Spring Boot 4.1.1 auf Java 21, REST-API unter `/api`
- **Datenbank:** MariaDB 11.4 im Docker-Container ([ADR-0001](docs/adr/0001-mariadb-als-datenbank.md))

## Schnellstart

Voraussetzung: Docker mit Compose-Plugin (Windows: Docker Desktop mit WSL 2).

```bash
docker-compose up -d --build
```

Dann <http://localhost> öffnen. Beim ersten Start werden 13 Beispielprodukte in 4 Kategorien
eingespielt. Der erste Build dauert ein paar Minuten (Maven und npm laden Abhängigkeiten).

| Seite | Adresse |
| --- | --- |
| Shop (Produktliste) | <http://localhost/> |
| Produktdetails | <http://localhost/produkte/1> |
| Pflege (Produkte und Kategorien) | <http://localhost/pflege> |

```bash
docker-compose ps          # Status, alle drei Container sollten laufen bzw. "healthy" sein
docker-compose logs -f     # Logs verfolgen
docker-compose down        # stoppen, Daten bleiben erhalten
docker-compose down -v     # stoppen UND Datenbank löschen → beim nächsten Start wieder Beispieldaten
```

## Sprint 1: Das Schaufenster

| Nr. | Anforderung | Wo im Code |
| --- | --- | --- |
| [0001](docs/anforderungen/0001-produktliste.md) | Produktliste mit Name, Preis, Bild, Suche, Kategorie-Filter, endlosem Scrollen | `frontend/src/app/pages/product-list/`, `ProductController#list` |
| [0002](docs/anforderungen/0002-produktdetails-bestand.md) | Produktdetails mit Bestandsstatus | `frontend/src/app/pages/product-detail/`, `StockStatus.java` |
| [0003](docs/anforderungen/0003-pflegeseite-produkte-kategorien.md) | Pflegeseite: anlegen, ändern (ganz und einzelne Felder), löschen | `frontend/src/app/pages/admin/`, `ProductController`, `CategoryController` |
| [0004](docs/anforderungen/0004-beispieldaten.md) | Beispielprodukte beim Start | `SampleDataLoader.java`, `frontend/public/images/` |
| [0005](docs/anforderungen/0005-fehlermeldungen.md) | Sinnvolle Fehlermeldungen | `GlobalExceptionHandler.java`, `frontend/src/app/shared/errors.ts` |

Architekturentscheidungen: [docs/adr/](docs/adr/README.md).
Erklärung des Codes fürs Review: [docs/sprint-1-spickzettel.md](docs/sprint-1-spickzettel.md).

## Wie eine Anfrage läuft

```
Browser ──▶ frontend (nginx :80) ──▶ index.html + JS/CSS + /images/*.svg
                    │
                    └──▶ /api/* ──▶ backend (Spring Boot :8080) ──▶ db (MariaDB :3306)
```

Das Frontend ruft nur **relative** Pfade wie `/api/products` auf; nginx leitet alles unter `/api/`
an das Backend weiter. Dadurch gibt es kein CORS, und die Ports 8080 und 3306 sind von außen nicht
erreichbar. Startreihenfolge über Healthchecks: `db` → `backend` → `frontend`.

## REST-API

| Methode | Pfad | Zweck |
| --- | --- | --- |
| GET | `/api/products?q=&categoryId=&page=0&size=12` | Produkte suchen, je 12 pro Ladung (endloses Scrollen) |
| GET | `/api/products/{id}` | Produktdetails inkl. `stockStatus` |
| POST | `/api/products` | Produkt anlegen |
| PUT | `/api/products/{id}` | ganzes Produkt ändern |
| PATCH | `/api/products/{id}` | einzelne Felder ändern, z. B. `{ "stock": 10 }` |
| DELETE | `/api/products/{id}` | Produkt löschen |
| GET | `/api/categories` | alle Kategorien inkl. `productCount` |
| POST / PUT / DELETE | `/api/categories[/{id}]` | Kategorie anlegen, umbenennen, löschen (nur wenn leer) |
| GET | `/api/health` | Healthcheck für Docker |

Fehler kommen immer als Problem Details (RFC 9457), siehe [ADR-0003](docs/adr/0003-validierung-im-backend-und-einheitliche-fehler.md):

```json
{ "status": 400, "title": "Ungültige Eingabe", "detail": "Bitte prüfe die markierten Felder.",
  "errors": [ { "field": "price", "message": "Der Preis muss größer als 0 sein." } ] }
```

## Tests

Beide Testsuites laufen automatisch im Docker-Build mit (`docker-compose up --build`). Auf dem Rechner:

```bash
cd backend  && ./mvnw test                      # JUnit + MockMvc gegen H2, braucht JDK 21
cd frontend && npm install && npx ng test       # Vitest
```

## Lokale Entwicklung (mit Hot Reload)

```bash
docker-compose up -d db                         # nur die Datenbank starten …
```

… dann in `docker-compose.yml` beim Service `db` vorübergehend `ports: ["3306:3306"]` ergänzen
(nicht committen), und:

```bash
cd backend  && ./mvnw spring-boot:run           # http://localhost:8080
cd frontend && npm start                        # http://localhost:4200
```

`ng serve` braucht dafür eine `proxy.conf.json`:
`{ "/api": { "target": "http://localhost:8080", "secure": false } }` und den Start mit
`npx ng serve --proxy-config proxy.conf.json`.

## Projektstruktur

```
docker-compose.yml        db + backend + frontend
docs/anforderungen/       User Stories mit Akzeptanzkriterien (Skill "anforderung")
docs/adr/                 Architekturentscheidungen (Skill "adr")
backend/src/main/java/de/hsaa/shop/
  product/                Produkt: Entity, Repository, Service, Controller, DTOs, StockStatus
  category/               Kategorie: Entity, Repository, Service, Controller, DTOs
  common/                 Fehlerbehandlung (Problem Details), Healthcheck
  sampledata/             Beispieldaten beim ersten Start
frontend/src/app/
  api/                    HTTP-Services und Datentypen
  shared/                 Formatierung, Fehlermeldungen, Platzhalterbild, Dialoge
  pages/                  product-list, product-detail, admin, not-found
frontend/public/images/   Produktbilder (SVG) und Platzhalter
```

## Firmennetz mit TLS-Interception

Schlägt der Backend-Build mit `PKIX path building failed` fehl, das Root-CA-Zertifikat als `.pem`
nach `backend/docker/certs/` legen (wird nicht committet), siehe `backend/docker/certs/README.md`.

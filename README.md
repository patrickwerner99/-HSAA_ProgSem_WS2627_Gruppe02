# Hyrox-Shop – E-Commerce-Hub (Gruppe 02)

Projektseminar Programmierung, Hochschule Aalen, WS 26/27.
Ein Online-Shop für Sport-Supplements, spezialisiert auf Hyrox: Kreatin, Protein, Aminosäuren
und unser eigenes Hyrox-Supplement, jeweils in mehreren Sorten.

- **Frontend:** Angular 22 mit Angular Material, als statische Dateien von nginx ausgeliefert
- **Backend:** Spring Boot 4.1.1 auf Java 21, REST-API unter `/api`
- **Datenbank:** MariaDB 11.4 im Docker-Container ([ADR-0001](docs/adr/0001-mariadb-als-datenbank.md))

## Schnellstart

Voraussetzung: Docker mit Compose (Windows: Docker Desktop mit WSL 2).

```bash
git clone <repo-url> && cd <repo-ordner>
docker compose up -d --build
```

Dann <http://localhost> öffnen. Beim ersten Start werden 31 Beispielprodukte in 5 Kategorien
und ein Admin-Konto angelegt. Der erste Build dauert ein paar Minuten (Maven und npm laden
Abhängigkeiten, die Backend-Tests laufen im Build mit).

### Admin-Zugang

| | |
| --- | --- |
| E-Mail | `admin@hyrox-shop.local` |
| Passwort | wird beim **ersten** Start zufällig erzeugt und steht im Backend-Log |

```powershell
docker compose logs backend | Select-String -Context 0,3 "Admin-Zugang"     # Windows PowerShell
docker compose logs backend | grep -A3 "Admin-Zugang"                       # macOS / Linux
```

Eigenes Passwort statt Zufall: neben `docker-compose.yml` eine Datei `.env` mit
`ADMIN_PASSWORD=dein-passwort` anlegen (steht in `.gitignore`, kommt nie ins Repo), dann
`docker compose down -v` und neu starten. Warum so: [ADR-0008](docs/adr/0008-admin-zugang-ohne-passwort-im-repo.md).

Kunden-Konten legt man im Shop selbst an („Anmelden“ → „Jetzt registrieren“).

| Seite | Adresse |
| --- | --- |
| Shop (Produktliste) | <http://localhost/> |
| Sortiert und gefiltert (Beispiel-Link) | <http://localhost/?sort=price-asc&verfuegbar=ja> |
| Produktdetails | <http://localhost/produkte/1> |
| Warenkorb | <http://localhost/warenkorb> |
| Meine Bestellungen | <http://localhost/bestellungen> |
| Sale | <http://localhost/sale> |
| Meine Retouren | <http://localhost/retouren> |
| Mein Konto (Daten, Passwort, Adressen) | <http://localhost/konto> |
| Pflege (nur Admin) | <http://localhost/pflege/produkte>, `/pflege/kategorien`, `/pflege/bestellungen`, `/pflege/retouren`, `/pflege/rabattcodes` |

Rabattcodes zum Ausprobieren: `HYROX10` (10 %), `WILLKOMMEN15` (15 %, max. 100×), `SOMMER20` (abgelaufen, für die
Fehlermeldung). Nährwerte aus Open Food Facts zeigt z. B. „Whey Protein Schoko“.

> **Schon einmal gestartet?** Neue Beispieldaten (Sortiment, Sale, Rabattcodes, Barcodes) kommen nur in eine leere
> Datenbank. Einmal `docker compose down -v` und dann `docker compose up -d --build` ausführen. Achtung: Das löscht
> auch eigene Testkonten und Bestellungen.

```bash
docker compose ps          # Status, alle drei Container sollten "healthy" sein
docker compose logs -f     # Logs verfolgen
docker compose down        # stoppen, Daten bleiben erhalten (Produkte, Konten, Bestellungen, Bilder)
docker compose down -v     # stoppen UND alles löschen → beim nächsten Start Beispieldaten + neues Admin-Passwort
```

## Sprint 3: Konto, Retouren, Sale, externe API

| Nr. | Anforderung | Vorgabe | Wo im Code |
| --- | --- | --- | --- |
| [0017](docs/anforderungen/0017-lieferadresse-beim-bestellen.md) | Lieferadresse beim Bestellen | Review 2 | `order/DeliveryAddress.java`, `pages/cart/` |
| [0018](docs/anforderungen/0018-packungsgroessen.md) | Packungsgrößen | Review 2 | `Product#size`, `pages/product-detail/` |
| [0019](docs/anforderungen/0019-produktart-filter.md) | Nach Produktart filtern | Review 2 | `ProductForm.java`, `shared/product-form.ts` |
| [0020](docs/anforderungen/0020-realistisches-sortiment.md) | Realistisches Sortiment | Review 2 | `SampleDataLoader.java` |
| [0021](docs/anforderungen/0021-kontodaten-aendern.md) | Eigene Kontodaten ändern | E1 | `account/AccountController.java`, `pages/account/account-settings.ts` |
| [0022](docs/anforderungen/0022-mehrere-adressen.md) | Mehrere Adressen, Kopie in der Bestellung | E2, E3 | `account/SavedAddress.java`, `OrderService#place`, `pages/cart/` |
| [0023](docs/anforderungen/0023-sale.md) | Sale mit Zeitraum und Ersparnis | G1 | `Product#currentPrice`, `ProductService#startSale`, Route `/sale` |
| [0024](docs/anforderungen/0024-rabattcodes.md) | Rabattcodes | G2 | `discount/`, `pages/admin/discount-codes-tab.ts` |
| [0025](docs/anforderungen/0025-retoure-anmelden.md) | Retoure anmelden, Stand sehen | F1 | `returns/`, `pages/orders/order-detail`, `my-returns.ts` |
| [0026](docs/anforderungen/0026-retouren-bearbeiten.md) | Retouren bearbeiten, reduzierte Ware | F2 | `ReturnService#update`, `pages/admin/returns-tab.ts` |
| [0027](docs/anforderungen/0027-naehrwerte-externe-api.md) | Nährwerte aus Open Food Facts | H1, H2 | `nutrition/`, `pages/product-detail/` |
| [0015](docs/anforderungen/0015-rest-api-und-http-datei.md) | Alle neuen Endpunkte in der `.http`-Datei | N1, N2 | [`http/api.http`](http/api.http) |

Neue ADRs: [0009](docs/adr/0009-lieferadresse-in-der-bestellung-speichern.md) Adresse als Kopie,
[0010](docs/adr/0010-retourenware-als-eigenes-produkt.md) Retourenware als eigenes Produkt,
[0011](docs/adr/0011-open-food-facts-als-externe-api.md) Wahl der externen API,
[0012](docs/adr/0012-externe-api-serverseitig-mit-timeout-und-cache.md) Anbindung mit Timeout, Cache und Ersatzanzeige.

## Sprint 2: Einkaufen

| Nr. | Anforderung | Vorgabe | Wo im Code |
| --- | --- | --- | --- |
| [0006](docs/anforderungen/0006-kundenkonto.md) | Kundenkonto | A1 | `account/`, `pages/account/`, `api/auth.service.ts` |
| [0007](docs/anforderungen/0007-admin-bereich-geschuetzt.md) | Admin-Bereich geschützt, auch per API | A2 | `security/SecurityConfig.java`, `shared/auth.ts` |
| [0008](docs/anforderungen/0008-warenkorb.md) | Warenkorb | B1 | `shared/cart.service.ts`, `pages/cart/` |
| [0009](docs/anforderungen/0009-bestellen.md) | Bestellen, Bestand, Preise vom Shop | B1–B3 | `order/OrderService.java#place` |
| [0010](docs/anforderungen/0010-meine-bestellungen.md) | Meine Bestellungen, stornieren | A1 | `pages/orders/` |
| [0011](docs/anforderungen/0011-bestellungen-verwalten.md) | Bestellungen verwalten | B4 | `pages/admin/orders-tab.ts`, `OrderStatus.java` |
| [0012](docs/anforderungen/0012-sortieren-filtern.md) | Suchen, filtern, sortieren | C1 | `ProductService#search`, `pages/product-list/` |
| [0013](docs/anforderungen/0013-ansichten-als-link.md) | Jede Ansicht als Link | C2 | `app.routes.ts`, Query-Parameter in den Seiten |
| [0014](docs/anforderungen/0014-produktbilder.md) | Produktbilder hochladen | D1–D3 | `image/`, `product-form-dialog.ts`, `fallback-image.directive.ts` |
| [0015](docs/anforderungen/0015-rest-api-und-http-datei.md) | REST-API, `.http`-Datei | N1, N2 | [`http/api.http`](http/api.http) |
| [0016](docs/anforderungen/0016-start-daten-geheimnisse.md) | Start, Daten, keine Geheimnisse | Q1–Q3 | `docker-compose.yml`, `AdminAccountInitializer.java` |

## Sprint 1: Das Schaufenster

| Nr. | Anforderung | Wo im Code |
| --- | --- | --- |
| [0001](docs/anforderungen/0001-produktliste.md) | Produktliste mit Name, Preis, Bild, Suche, Kategorie-Filter, endlosem Scrollen | `frontend/src/app/pages/product-list/`, `ProductController#list` |
| [0002](docs/anforderungen/0002-produktdetails-bestand.md) | Produktdetails mit Bestandsstatus | `frontend/src/app/pages/product-detail/`, `StockStatus.java` |
| [0003](docs/anforderungen/0003-pflegeseite-produkte-kategorien.md) | Pflegeseite: anlegen, ändern (ganz und einzelne Felder), löschen | `frontend/src/app/pages/admin/`, `ProductController`, `CategoryController` |
| [0004](docs/anforderungen/0004-beispieldaten.md) | Beispielprodukte beim Start | `SampleDataLoader.java`, `frontend/public/images/` |
| [0005](docs/anforderungen/0005-fehlermeldungen.md) | Sinnvolle Fehlermeldungen | `GlobalExceptionHandler.java`, `frontend/src/app/shared/errors.ts` |

Architekturentscheidungen: [docs/adr/](docs/adr/README.md).
Erklärung des Codes fürs Review: [Sprint 1](docs/sprint-1-spickzettel.md), [Sprint 2](docs/sprint-2-spickzettel.md).

## Wie eine Anfrage läuft

```
Browser ──▶ frontend (nginx :80) ──▶ index.html + JS/CSS + /images/*.svg
                    │
                    └──▶ /api/* ──▶ backend (Spring Boot :8080) ──▶ db (MariaDB :3306)
                                         │
                                         └──▶ /data/images (Volume, hochgeladene Bilder)
```

Das Frontend ruft nur **relative** Pfade wie `/api/products` auf; nginx leitet alles unter `/api/`
an das Backend weiter. Dadurch gibt es kein CORS, und die Ports 8080 und 3306 sind von außen nicht
erreichbar. Startreihenfolge über Healthchecks: `db` → `backend` → `frontend`.

## REST-API

Alle Endpunkte mit Erfolgs- und Fehlerfall zum Ausprobieren: [`http/api.http`](http/api.http)
(IntelliJ/WebStorm: Umgebung „dev“ wählen, „Run All“; vorher Admin-Passwort in
`http/http-client.private.env.json` eintragen, Vorlage liegt daneben).

| Methode | Pfad | Wer | Zweck / Statuscodes |
| --- | --- | --- | --- |
| GET | `/api/products?q=&categoryId=&form=&sale=&sort=&inStock=&page=0&size=12` | alle | Produkte suchen, filtern (auch nach Produktart `form`: `PULVER`, `RIEGEL`, `GETRAENK`, `KAPSELN`, `GEL`, und `sale=true`), sortieren (`name`, `price-asc`, `price-desc`, `newest`, `saving`) · 200, 400 |
| GET | `/api/products/{id}` | alle | Produkt inkl. `stock`, `stockStatus`, `imageUrl`, `size`, `form`, `ean`, `price` (heute), `regularPrice`, `onSale`, `savingPercent` · 200, 404 |
| GET | `/api/products/{id}/nutrition` | alle | Nährwerte aus Open Food Facts · 200, 404 (kein Barcode), 503 (API nicht erreichbar) |
| PUT / DELETE | `/api/products/{id}/sale` | Admin | Sale `{ "salePrice", "from", "until" }` setzen / beenden · 200, 400, 404 |
| POST | `/api/products` | Admin | anlegen · 201 + Location, 400, 401, 403 |
| PUT | `/api/products/{id}` | Admin | ganz ändern · 200, 400, 404 |
| PATCH | `/api/products/{id}` | Admin | einzelne Felder, z. B. `{ "stock": 10 }` · 200, 400, 404 |
| DELETE | `/api/products/{id}` | Admin | löschen · 204, 404 |
| POST | `/api/products/{id}/image` | Admin | Bild hochladen (multipart `file`, PNG/JPG/WebP/GIF, max. 5 MB) · 200, 400, 413 |
| DELETE | `/api/products/{id}/image` | Admin | Bild entfernen · 200 |
| GET | `/api/images/{name}` | alle | Bilddatei · 200, 404 |
| GET | `/api/categories`, `/api/categories/{id}` | alle | Kategorien inkl. `productCount` · 200, 404 |
| POST / PUT / DELETE | `/api/categories[/{id}]` | Admin | anlegen, umbenennen, löschen (nur wenn leer) · 201/200/204, 400, 409 |
| POST | `/api/auth/register` | alle | Konto anlegen und anmelden · 201, 400, 409 |
| POST | `/api/auth/login` | alle | anmelden (Sitzungs-Cookie) · 200, 401 |
| POST | `/api/auth/logout` | alle | abmelden · 204 |
| GET | `/api/auth/me` | angemeldet | wer bin ich · 200, 401 |
| POST | `/api/orders` | angemeldet | bestellen `{ "items": [{ "productId", "quantity" }], "deliveryAddress": { "name", "street", "postalCode", "city" } }` oder `"addressId"`, optional `"discountCode"` · 201 + Location, 400, 401, 409 |
| GET | `/api/orders?status=&page=&size=` | angemeldet | Kunde: eigene, Admin: alle · 200 |
| GET | `/api/orders/{id}` | angemeldet | eine Bestellung · 200, 404 |
| PATCH | `/api/orders/{id}` | angemeldet | Status `{ "status": "PROCESSING" }`; Kunde nur Storno · 200, 400, 403, 409 |
| GET / PATCH | `/api/account` | angemeldet | eigene Daten lesen / Name, E-Mail ändern (E-Mail nur mit `currentPassword`) · 200, 400, 401, 409 |
| PUT | `/api/account/password` | angemeldet | Passwort ändern · 204, 400 |
| GET / POST | `/api/account/addresses` | angemeldet | gespeicherte Adressen (max. 10) · 200 / 201, 400, 409 |
| PUT / DELETE | `/api/account/addresses/{id}` | angemeldet | ändern (auch Standard) / löschen · 200 / 204, 400, 404 |
| GET / POST | `/api/discount-codes` | Admin | Rabattcodes · 200 / 201, 400, 409 |
| PATCH / DELETE | `/api/discount-codes/{id}` | Admin | abschalten, verlängern / löschen (nur unbenutzt) · 200 / 204, 404, 409 |
| GET | `/api/discount-codes/{code}` | angemeldet | gilt der Code heute? · 200, 404, 409 |
| POST | `/api/returns` | angemeldet | Retoure `{ "orderId", "reason", "items": [{ "productId", "quantity" }] }` · 201, 400, 404, 409 |
| GET | `/api/returns?status=&page=&size=`, `/api/returns/{id}` | angemeldet | Kunde: eigene, Admin: alle · 200, 404 |
| GET | `/api/orders/{orderId}/returns` | angemeldet | Retouren einer Bestellung · 200 |
| PATCH | `/api/returns/{id}` | Admin | Status, bei `RECEIVED` mit `conditions` (`UNUSED`/`USED`) · 200, 400, 403, 404, 409 |
| GET | `/api/health` | alle | Healthcheck für Docker |

Fehler kommen immer als Problem Details (RFC 9457), auch 401/403, siehe [ADR-0003](docs/adr/0003-validierung-im-backend-und-einheitliche-fehler.md):

```json
{ "status": 400, "title": "Ungültige Eingabe", "detail": "Bitte prüfe die markierten Felder.",
  "errors": [ { "field": "price", "message": "Der Preis muss größer als 0 sein." } ] }
```

## Tests

Die Backend-Tests laufen bei jedem `docker compose up --build` mit (schlägt einer fehl, baut das
Image nicht). Auf dem Rechner:

```bash
cd backend  && ./mvnw test                      # JUnit + MockMvc gegen H2, braucht JDK 21
cd frontend && npm install && npx ng test       # Vitest
```

| Bereich | Backend | Frontend |
| --- | --- | --- |
| Konto, Rechte (A1, A2) | `AuthApiTests`, `AccessRulesTests` | `auth.spec.ts` |
| Bestellen, Bestand, Preise, Status (B1–B4) | `OrderApiTests` | `cart.spec.ts`, `cart.service.spec.ts` |
| Suchen, Filtern, Sortieren (C1, C2) | `ProductApiTests` | `product-list.spec.ts` |
| Bilder (D1–D3) | `ImageApiTests` | `product-list.spec.ts` (Platzhalter) |
| Konto, Adressen (E1–E3) | `AccountApiTests` | `cart.spec.ts` |
| Sale, Rabattcodes (G1, G2) | `SaleApiTests`, `DiscountApiTests` | `cart.spec.ts`, `product-list.spec.ts` |
| Retouren (F1, F2) | `ReturnApiTests` | – |
| Externe API (H1, H2) | `NutritionApiTests` (eigener Test-Server statt Open Food Facts) | `product-detail.spec.ts` |
| Sprint 1 | `ProductApiTests`, `CategoryApiTests`, `StockStatusTests` | `format.spec.ts`, `errors.spec.ts` |

## Lokale Entwicklung (mit Hot Reload)

```bash
docker compose up -d db                         # nur die Datenbank starten …
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
docker-compose.yml        db + backend + frontend, Volumes db-data und images
http/api.http             alle Endpunkte zum Ausprobieren (N2)
docs/anforderungen/       User Stories mit Akzeptanzkriterien
docs/adr/                 Architekturentscheidungen
backend/src/main/java/de/hsaa/shop/
  product/                Produkt: Entity, Repository, Service, Controller, DTOs, StockStatus
  category/               Kategorie
  account/                Konten, Rollen, Anmeldung, Admin beim ersten Start
  security/               Spring Security: wer darf was (A2)
  order/                  Bestellungen, Status, Bestandsprüfung, Lieferadresse
  discount/               Rabattcodes (G2)
  returns/                Retouren (F1, F2)
  nutrition/              Nährwerte aus Open Food Facts (H1, H2)
  image/                  Bilder hochladen und ausliefern
  common/                 Fehlerbehandlung (Problem Details), Seiten, Healthcheck
  sampledata/             Beispieldaten beim ersten Start
frontend/src/app/
  api/                    HTTP-Services und Datentypen
  shared/                 Warenkorb, Guards, Formatierung, Fehlermeldungen, Platzhalterbild, Dialoge
  pages/                  product-list, product-detail, cart, account, orders, admin, …
frontend/public/images/   Beispiel-Produktbilder (SVG) und Platzhalter
```

## Firmennetz mit TLS-Interception

Schlägt der Backend-Build mit `PKIX path building failed` fehl, das Root-CA-Zertifikat als `.pem`
nach `backend/docker/certs/` legen (wird nicht committet), siehe `backend/docker/certs/README.md`.

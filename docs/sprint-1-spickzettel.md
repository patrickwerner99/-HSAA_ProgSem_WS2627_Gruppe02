# Spickzettel Sprint 1: So funktioniert unser Code

Für das Review („Code Ownership“): Jeder im Team sollte die folgenden Abläufe erklären können.

## 1. Der Weg einer Anfrage

Beispiel: Jemand öffnet die Startseite.

1. **Browser** lädt `index.html` und das JavaScript von **nginx** (Container `frontend`, Port 80).
2. Angular startet, die Route `''` zeigt `ProductListPage` (`pages/product-list/product-list.ts`).
3. Die Seite ruft `ProductService.search()` auf → HTTP `GET /api/products?page=0&size=12`.
4. **nginx** sieht `/api/` und leitet an `http://backend:8080` weiter (`frontend/nginx.conf`).
5. **Spring Boot**: `ProductController.list()` → `ProductService.search()` → `ProductRepository.findByNameContainingIgnoreCase(...)`.
6. **Spring Data JPA** erzeugt aus dem Methodennamen das SQL, **Hibernate** führt es auf **MariaDB** aus.
7. Die Entities werden in `ProductResponse`-Records umgewandelt (inkl. `stockStatus`) und als JSON zurückgegeben.
8. Angular zeigt die 12 Kacheln an. Kommt man ans Listenende, holt sich die Seite die nächsten 12 (`page=1`).

**Warum relative URLs (`/api/...`)?** Der Browser spricht nur mit einer Adresse (nginx). Dadurch gibt es kein CORS-Problem, und Backend und Datenbank sind von außen nicht erreichbar.

## 2. Backend in Schichten

| Schicht | Klasse (Beispiel Produkt) | Aufgabe |
| --- | --- | --- |
| Controller | `ProductController` | HTTP: URL, Methode, Parameter, Statuscodes (201, 204) |
| DTO | `ProductRequest`, `ProductPatch`, `ProductResponse` | Was rein- und rausgeht. Validierung per Annotation |
| Service | `ProductService` | Geschäftslogik, `@Transactional` |
| Repository | `ProductRepository` | Datenbankzugriff, Abfragen aus Methodennamen |
| Entity | `Product` | Tabelle `product` in der Datenbank |

**Warum DTOs statt die Entity direkt zurückgeben?** Wir bestimmen selbst, was nach außen geht (z. B. `categoryName` statt des ganzen Category-Objekts), und Änderungen an der Datenbank brechen nicht automatisch die API.

**PUT oder PATCH?** PUT = ganzes Produkt ersetzen (Formular). PATCH = nur mitgeschickte Felder ändern (Preis oder Bestand direkt in der Tabelle). Genau das verlangt die Vorgabe: „für ein ganzes Produkt und für einzelne Felder“.

**Warum ist der Preis ein `BigDecimal`?** Mit `double` wird aus 0,1 + 0,2 schnell 0,30000000000000004. Bei Geld darf das nicht passieren.

**Warum ist der Bestand im Request ein `BigDecimal`, in der Entity aber ein `int`?** Damit „3.5“ als Fehler abgelehnt wird, statt dass JSON-Parser es still auf 3 abrunden. Gespeichert wird eine ganze Zahl.

## 3. Fehlerbehandlung (ADR-0003)

- Ungültige Eingabe → Bean Validation schlägt an → `MethodArgumentNotValidException`.
- Eigene Regeln → wir werfen `NotFoundException` (404), `ConflictException` (409), `InvalidFieldException` (400).
- `GlobalExceptionHandler` (`@RestControllerAdvice`) fängt **alles** und baut daraus ein `ProblemDetail`
  mit `status`, `title`, `detail` und bei Feldfehlern einer Liste `errors`.
- Unerwartete Fehler → 500 mit allgemeiner Meldung, Details nur im Log.
- Im Frontend übersetzt `shared/errors.ts` das für den Nutzer: `userMessage()` für Meldungen,
  `fieldErrors()` für Fehler am Formularfeld. Status 0/502/503/504 = „Shop nicht erreichbar“.

**Warum prüfen wir im Frontend *und* im Backend?** Frontend = schnelle Rückmeldung am Feld. Backend = verbindlich, weil man die API auch ohne Formular aufrufen kann.

## 4. Endloses Scrollen (ADR-0002)

- Unter der Liste liegt ein unsichtbares `<div #sentinel>`. Ein `IntersectionObserver` meldet,
  wenn es in Sichtweite kommt → `loadMore()` → nächste Seite.
- Das Backend sagt mit `last: true`, dass nichts mehr kommt → „Alle Produkte angezeigt“.
- Nachteil „Position geht verloren“: `ProductListStore` merkt sich Liste und Scroll-Position;
  beim Zurück-Button wird beides wiederhergestellt.
- Suche und Kategorie stehen in der URL (`?q=whey&kategorie=2`): teilbar, und Neuladen behält den Filter.
- Suche erst nach 300 ms Tipp-Pause (`debounceTime`), sonst eine Anfrage pro Buchstabe.

## 5. Bestand (Anforderung 0002)

`StockStatus.of(stock)`: 0 → `SOLD_OUT`, 1–5 → `LOW_STOCK`, sonst `IN_STOCK`. Die Schwelle steht
**nur im Backend**, das Frontend übersetzt nur in Text und Farbe (`shared/format.ts`).
Kunden sehen den Status, die Pflegeseite die genaue Zahl.

## 6. Beispieldaten (Anforderung 0004)

`SampleDataLoader` ist ein `ApplicationRunner`: läuft einmal nach dem Start. Sind schon Kategorien
da, passiert nichts, so werden Änderungen nie überschrieben. Zurücksetzen: `docker-compose down -v`.

## 7. Docker

- 3 Container: `db` (MariaDB), `backend` (Spring Boot), `frontend` (nginx mit Angular-Build).
- Reihenfolge per Healthcheck: Backend startet erst, wenn die DB Verbindungen annimmt; nginx erst,
  wenn `/api/health` antwortet.
- Daten liegen im Volume `db-data`.
- Die Tests laufen beim Bauen mit; im Docker-Build gibt es keine MariaDB, deshalb nutzen die
  Backend-Tests H2 im Arbeitsspeicher (`src/test/resources/application.properties`).

## 8. Wahrscheinliche Fragen im Review

| Frage | Kurze Antwort |
| --- | --- |
| Warum MariaDB und nicht H2? | Relational, transaktionssicher, nah an echter Produktion; läuft per Docker überall gleich. Begründung in ADR-0001. |
| Was passiert, wenn ein Bild fehlt? | Leere URL → Platzhalter. Kaputte URL → die Direktive `appFallbackImage` tauscht das Bild beim Ladefehler gegen den Platzhalter. |
| Wie wird der Preis formatiert? | Angular `currency`-Pipe mit deutscher Locale → „34,90 €“. Eingabe akzeptiert Komma und Punkt. |
| Was passiert bei zwei gleichen Kategorienamen? | Service prüft `existsByNameIgnoreCase` → 409 „Diese Kategorie gibt es schon.“ |
| Warum kann man eine Kategorie mit Produkten nicht löschen? | Sonst hingen Produkte ohne Kategorie in der Luft. Lieber verbieten mit klarer Meldung. |
| Wie testet ihr? | Backend: `ProductApiTests` und `CategoryApiTests` (MockMvc, H2), `StockStatusTests`. Frontend: Vitest für Formatierung, Fehlertexte, Service und Produktliste. |
| Was ist `@Transactional` im Test? | Jede Änderung im Test wird danach zurückgerollt, damit Tests sich nicht gegenseitig beeinflussen. |
| Wo ist Login/Rollen? | Bewusst noch nicht in Sprint 1; steht als offener Punkt in Anforderung 0003. |

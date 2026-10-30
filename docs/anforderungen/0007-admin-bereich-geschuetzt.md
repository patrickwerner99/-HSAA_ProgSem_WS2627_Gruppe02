---
status: "erfüllt"
prioritaet: "Must"
sprint: "Sprint 2"
quelle: "Vorgabe Sprint 2, A2; Feedback aus Sprint 1 (Pflegeseite war offen)"
adr: "ADR-0004, ADR-0008"
---

# Admin-Bereich geschützt, auch bei direktem API-Aufruf

## Grobe Anforderung (Vorgabe)

> A2: Was nur Admins dürfen, ist für alle anderen gesperrt, auch beim direkten Aufruf der API.

## UI/UX-Überlegung

* „Pflege“ erscheint in der Kopfzeile nur für Admins. Öffnet ein Gast einen Link auf `/pflege/...`, kommt er zur Anmeldung; ein Kunde sieht „Diese Seite ist nur für Admins.“
* **Entscheidung und Begründung:** Das Ausblenden im Frontend ist nur Komfort. Die echte Sperre sitzt im Backend (Spring Security), denn jeder kann die API auch ohne unsere Oberfläche aufrufen (z. B. mit der `.http`-Datei oder curl).

## User Story

Als **Shop-Betreiber** möchte ich, dass nur Admins Produkte, Kategorien, Bilder und Bestellstatus ändern können, damit Kunden oder Fremde nichts kaputt machen.

## Akzeptanzkriterien

* Lesen (`GET /api/products`, `/api/categories`, `/api/images/...`) ist für alle offen.
* Ändern (`POST/PUT/PATCH/DELETE` auf Produkte, Kategorien, Bilder) ohne Anmeldung → **401** „Bitte melde dich an.“
* Dasselbe als angemeldeter Kunde → **403** „Das darf nur ein Admin.“
* Als Admin → wie bisher 200/201/204.
* Beide Fehler kommen im gleichen Problem-Details-Format wie alle anderen Fehler (ADR-0003).
* Es gibt beim ersten Start genau einen Admin; sein Passwort steht **nicht** im Repo (ADR-0008).

## Tests

`AccessRulesTests` (401/403/201), `ImageApiTests.customersCannotUploadImages`, `OrderApiTests.customerCannotSetOtherStatusesOrCancelLate`, Frontend `auth.spec.ts` (Guards).

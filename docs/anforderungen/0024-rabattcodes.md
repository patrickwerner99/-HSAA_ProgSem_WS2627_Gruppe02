---
status: "erfüllt"
prioritaet: "Must"
sprint: "Sprint 3"
quelle: "Sprint 3, G2"
adr: "ADR-0006"
---

# Rabattcodes

## Grobe Anforderung (Vorgabe)

> G2: Der Admin legt Rabattcodes an; Kunden lösen sie beim Bestellen ein.

## UI/UX-Überlegung

* **Seiten:** Pflegeseite → Reiter „Rabattcodes“; Warenkorb → Feld „Rabattcode“.
* Ein Code hat Prozent (1–50), Gültig von/bis, optional eine Höchstzahl an Einlösungen und kann abgeschaltet werden.
* Im Warenkorb prüft „Einlösen“ den Code sofort und zeigt Zwischensumme, Rabatt und Gesamtsumme. Ob er gilt, entscheidet
  trotzdem erst das Backend beim Bestellen (ADR-0005).
* **Entscheidung:** Ein schon benutzter Code kann nicht gelöscht, nur abgeschaltet werden, damit alte Bestellungen
  nachvollziehbar bleiben. Code, Prozent und Betrag werden in die Bestellung kopiert (E3).
* Groß-/Kleinschreibung egal: `hyrox10` = `HYROX10`.
* Der Zähler wird beim Bestellen unter Sperre erhöht (wie der Bestand, ADR-0006), damit ein Code mit Limit 1 nicht
  zweimal gleichzeitig eingelöst wird.

## User Story

Als **Admin** möchte ich Rabattcodes für Aktionen anlegen. Als **Kunde** möchte ich einen Code beim Bestellen
eingeben und sehen, wie viel ich spare.

## Akzeptanzkriterien

### Erwartetes Verhalten

* Admin: `GET/POST /api/discount-codes`, `PATCH/DELETE /api/discount-codes/{id}`.
* Angemeldet: `GET /api/discount-codes/{code}` → 200 mit Prozent, wenn der Code heute gilt.
* `POST /api/orders` mit `discountCode` → Bestellung mit `subtotal`, `discountPercent`, `discountAmount`, `total`.
* Beispielcodes: `HYROX10` (10 %), `WILLKOMMEN15` (15 %, max. 100×), `SOMMER20` (abgelaufen, zum Testen).

### Fehlerfälle

* Code doppelt → **409**. Prozent außerhalb 1–50, Ende vor Start → **400** mit Feldfehler.
* Code prüfen: unbekannt → **404**; abgelaufen, abgeschaltet, aufgebraucht → **409** mit Grund.
* Bestellung mit ungültigem Code → **400**, Feldfehler `discountCode`; es wird nichts bestellt und kein Bestand abgezogen.
* Benutzten Code löschen → **409**. Kunde verwaltet Codes → **403**.

## Tests

`DiscountApiTests` (alle 10 Tests), `cart.spec.ts` (Rabattcode einlösen und entfernen).

---
status: "erfüllt"
prioritaet: "Must"
sprint: "Sprint 3"
quelle: "Sprint 3, F1"
adr: "–"
---

# Retoure anmelden und Stand verfolgen

## Grobe Anforderung (Vorgabe)

> F1: Kunden können Ware aus einer gelieferten Bestellung zurückschicken und sehen jederzeit den Stand der Retoure.

## UI/UX-Überlegung

* **Seiten:** Bestelldetails → „Artikel zurückschicken“; Navigation „Retouren“ (`/retouren`).
* Der Knopf erscheint nur bei Bestellungen mit Status „Geliefert“ und innerhalb von 30 Tagen.
* Pro Position wählt der Kunde die Menge (0 bis „noch zurückschickbar“) und gibt einen Grund an.
  Teilretouren sind möglich; wer schon 1 von 3 zurückgeschickt hat, kann noch 2 zurückschicken.
* Hinweis im Formular: Zurückgenommen wird nur **originalverschlossene** Ware; Geöffnetes nur bei Mängeln
  (Lebensmittel, Hygiene).
* Der Stand wird als Schrittanzeige gezeigt: **Angemeldet → Eingegangen → Erstattet** (oder „Abgelehnt“ mit Hinweis
  des Admins). Dazu der voraussichtliche Erstattungsbetrag.

## User Story

Als **Kunde** möchte ich einzelne Artikel aus einer gelieferten Bestellung zurückschicken und sehen, wie weit meine
Retoure ist, damit ich weiß, wann ich mein Geld bekomme.

## Akzeptanzkriterien

### Erwartetes Verhalten

* `POST /api/returns` mit `orderId`, `reason` und `items` (`productId`, `quantity`) → 201, Status `REQUESTED`.
* `GET /api/returns` (eigene Retouren, seitenweise, Filter `status`), `GET /api/returns/{id}`,
  `GET /api/orders/{orderId}/returns`.
* Der erwartete Erstattungsbetrag rechnet mit den Preisen und dem Rabatt der Bestellung (E3).

### Fehlerfälle

* Bestellung nicht geliefert oder älter als 30 Tage → **409**.
* Mehr zurückschicken als bestellt (minus schon angemeldet) → **409** mit „Von „…“ kannst du höchstens N Stück
  zurückschicken.“
* Grund fehlt, keine Position mit Menge > 0 → **400** mit Feldfehler.
* Fremde Bestellung oder Retoure → **404**. Nicht angemeldet → **401**.
* Abgelehnte Retouren zählen nicht mit; die Ware kann erneut angemeldet werden.

## Tests

`ReturnApiTests` (customerReturnsPartOfADeliveredOrder, onlyDeliveredOrdersCanBeReturned,
cannotReturnMoreThanWasOrdered, foreignOrdersAndReturnsAreInvisible, missingReasonIsRejected,
rejectedReturnsDoNotCountAgainstTheQuantity).

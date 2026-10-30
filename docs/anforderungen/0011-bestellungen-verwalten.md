---
status: "erfüllt"
prioritaet: "Must"
sprint: "Sprint 2"
quelle: "Vorgabe Sprint 2, B4"
adr: "keine"
---

# Bestellungen verwalten (Admin)

## Grobe Anforderung (Vorgabe)

> B4: Admins sehen Bestellungen und bearbeiten Status.

## UI/UX-Überlegung

* Neuer Reiter `/pflege/bestellungen`: Tabelle mit Nr., Datum, Kunde, Artikel, Summe, Status und einem Menü „Status setzen“. Filter nach Status, Seite in der URL (`?status=RECEIVED&seite=2`).
* Klick auf die Nummer öffnet die Bestellung mit allen Positionen.
* **Entscheidung und Begründung:** Feste Reihenfolge der Status, damit nichts Unsinniges passiert (z. B. „Zugestellt“ → „Eingegangen“):

```
Eingegangen ──► In Bearbeitung ──► Versendet ──► Zugestellt
     │                 │
     └──► Storniert ◄──┘          (Storniert gibt den Bestand zurück)
```

## User Story

Als **Admin** möchte ich alle Bestellungen sehen und ihren Status weitersetzen, damit Kunden wissen, wo ihre Bestellung steht.

## Akzeptanzkriterien

* Admin sieht Bestellungen aller Kunden, neueste zuerst, filterbar nach Status.
* Im Menü stehen nur die erlaubten nächsten Status.
* Unerlaubter Übergang über die API (z. B. „Versendet“ → „Storniert“) → **409** mit Erklärung.
* Unbekannter Status (`"VERLOREN"`) → **400**.
* Stornieren gibt den Bestand zurück.

## Tests

`OrderApiTests.adminSeesAllOrdersAndCanFilterByStatus`, `adminMovesOrderThroughAllowedStatuses`, `unknownStatusIsRejected`.

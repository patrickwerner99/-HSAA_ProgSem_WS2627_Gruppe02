---
status: "erfüllt"
prioritaet: "Must"
sprint: "Sprint 2"
quelle: "Vorgabe Sprint 2, C1"
adr: "ADR-0002"
---

# Suchen, filtern, sortieren

## Grobe Anforderung (Vorgabe)

> C1: suchen, filtern, sortieren.

## UI/UX-Überlegung

* Suche und Kategorie gab es seit Sprint 1 (0001). Neu: Auswahl „Sortieren nach“ (Name A–Z, Preis aufsteigend, Preis absteigend, Neuheiten zuerst) und Schalter „Nur verfügbare“.
* Sortiert wird im Backend, nicht im Browser, weil das endlose Scrollen immer nur 12 Produkte lädt (ADR-0002); im Browser sortiert wären nur die ersten 12 richtig.

## User Story

Als **Kunde** möchte ich die Produkte nach Preis oder Neuheit sortieren und Ausverkauftes ausblenden, damit ich schneller finde, was ich kaufen kann.

## Akzeptanzkriterien

* `GET /api/products?sort=price-asc|price-desc|name|newest`; Standard ist `name`. Bei gleichem Preis entscheidet der Name, damit die Reihenfolge stabil ist (wichtig fürs Nachladen).
* `GET /api/products?inStock=true` blendet Produkte mit Bestand 0 aus.
* Sortierung, Filter und Suche lassen sich kombinieren.
* Unbekannte Sortierung über die API (`sort=billig`) → **400** mit Feldfehler `sort`. Im Frontend fällt eine unbekannte Sortierung in der URL still auf „Name“ zurück.
* Bei wenig Bestand zeigt die Kachel „Nur noch N“.

## Tests

`ProductApiTests` (sortByPrice…, newestFirst, inStockOnlyHidesSoldOutProducts, unknownSortIsRejected), `product-list.spec.ts`.

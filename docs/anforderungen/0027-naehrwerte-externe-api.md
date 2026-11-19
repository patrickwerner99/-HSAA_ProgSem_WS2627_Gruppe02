---
status: "erfüllt"
prioritaet: "Must"
sprint: "Sprint 3"
quelle: "Sprint 3, H1 und H2"
adr: "ADR-0011, ADR-0012"
---

# Nährwerte aus Open Food Facts (externe API)

## Grobe Anforderung (Vorgabe)

> H1: Der Shop zeigt Inhalte aus mindestens einer externen API, die zum Shop passen.
> H2: Der Shop bleibt nutzbar, wenn die API langsam oder nicht erreichbar ist.

## UI/UX-Überlegung

* **Seite:** Produktdetails, Abschnitt „Nährwerte pro 100 g“ unter der Beschreibung; Pflegeseite: Feld „Barcode / EAN“.
* Für Sportnahrung sind Eiweiß, Kalorien und Zucker die wichtigsten Kaufkriterien. Open Food Facts ist eine offene
  Lebensmitteldatenbank mit genau diesen Werten (ADR-0011).
* Ehrlich zur Herkunft: Unter der Tabelle steht „Quelle: Open Food Facts (Vergleichsprodukt „…“ von …)“, mit Link.
  Unser Shop ist ausgedacht; in den Beispieldaten hängt am „Whey Protein Schoko“ der Barcode eines echten,
  vergleichbaren Produkts.
* Die Produktseite wartet **nicht** auf die Nährwerte: Erst kommt das Produkt, dann lädt der Abschnitt nach
  („Nährwerte werden geladen …“).
* Ist Open Food Facts weg: Waren die Werte schon einmal geladen, zeigen wir die gespeicherten mit „Stand …, gerade nicht
  aktualisierbar“. Sonst steht dort „Nährwerte sind gerade nicht verfügbar“. Kaufen geht in jedem Fall.
* Produkte ohne Barcode zeigen keinen Nährwert-Abschnitt.

## User Story

Als **Kunde** möchte ich die Nährwerte eines Produkts sehen, damit ich vergleichen kann, wie viel Eiweiß und Zucker drin
ist, bevor ich kaufe.

## Akzeptanzkriterien

### Erwartetes Verhalten

* `GET /api/products/{id}/nutrition` → 200 mit `energyKcal`, `protein`, `carbohydrates`, `sugars`, `fat`,
  `saturatedFat`, `salt` (pro 100 g), `productName`, `brands`, `source`, `sourceUrl`, `fetchedAt`, `stale`.
* Das Backend fragt Open Food Facts, nicht der Browser (ADR-0012). Antworten werden 24 Stunden gespeichert.
* Der Admin kann pro Produkt einen Barcode (EAN-8 oder 12–14 Ziffern) eintragen; leer = keine Nährwerte.

### Fehlerfälle

* Produkt ohne Barcode → **404** „Für dieses Produkt sind keine Nährwerte hinterlegt.“
* Open Food Facts kennt den Barcode nicht → **404**.
* Open Food Facts antwortet nicht innerhalb von 2,5 s oder mit Fehler, nichts gespeichert → **503** „Gerade nicht
  verfügbar“. Mit gespeicherten Werten → **200** mit `stale: true`.
* `GET /api/products/{id}` funktioniert immer, auch wenn Open Food Facts weg ist.
* Ungültiger Barcode beim Speichern → **400**, Feldfehler `ean`.

## Tests

`NutritionApiTests` (8 Tests; ein kleiner Test-Server ersetzt Open Food Facts und spielt Antwort, unbekannten
Barcode, Ausfall und langsame Antwort durch), `product-detail.spec.ts` (Nährwerte anzeigen, 503 → Hinweis und
weiter kaufbar, ohne Barcode keine Anfrage).

---
status: "erfüllt"
prioritaet: "Must"
sprint: "Sprint 3"
quelle: "Sprint 3, F2"
adr: "ADR-0010"
---

# Retouren bearbeiten, originalverschlossene Ware reduziert verkaufen

## Grobe Anforderung (Vorgabe)

> F2: Der Admin bearbeitet Retouren. Unbenutzte zurückgeschickte Ware kommt als reduzierte Ware wieder in den Verkauf.

## UI/UX-Überlegung

* **Seite:** Pflegeseite → Reiter „Retouren“, Filter nach Status, neueste zuerst.
* Ablauf: **Angemeldet → Eingegangen → Erstattet**; „Abgelehnt“ geht aus „Angemeldet“ und „Eingegangen“.
* Bei „Eingegangen“ wählt der Admin pro Position **Originalverschlossen** (Siegel intakt) oder **Geöffnet / beschädigt**.
  Erst dann wird gebucht. In der API heißen die Werte `UNUSED` und `USED`.
* **Entscheidung – Lebensmittel:** „Unbenutzt“ allein reicht bei Lebensmitteln nicht. Wieder verkauft wird nur, was
  **originalverschlossen** ist, denn nur dann ist sicher, dass niemand an den Inhalt gekommen ist.
* **Entscheidung (ADR-0010):** Originalverschlossene Ware wird nicht zum normalen Bestand gezählt, sondern als eigenes Produkt
  „Retourenware“ geführt, 30 % reduziert (Sale ohne Enddatum). So sieht der Kunde, dass es Rückläufer sind, und der
  normale Preis bleibt unangetastet. Gibt es das Retourenprodukt schon, wird nur der Bestand erhöht.
* Geöffnete Ware (z. B. angebrochenes Pulver) wird aus Hygienegründen nicht wieder verkauft.

## User Story

Als **Admin** möchte ich eingegangene Retouren prüfen und abschließen, damit Kunden ihr Geld bekommen und originalverschlossene
Ware nicht verloren geht.

## Akzeptanzkriterien

### Erwartetes Verhalten

* Admin: `GET /api/returns` zeigt alle Retouren; `PATCH /api/returns/{id}` mit `status` (und `conditions`, `note`).
* `RECEIVED` + Zustand `UNUSED` (originalverschlossen) → Retourenprodukt mit gleicher Kategorie, Art, Größe; `returnedGoods = true`,
  Preis −30 %, Bestand = zurückgeschickte Menge. Es erscheint im Sale.
* `REFUNDED` setzt den erstatteten Betrag (Preise und Rabatt der Bestellung).

### Fehlerfälle

* Kunde ändert Status → **403**.
* Unerlaubter Übergang (z. B. `REQUESTED → REFUNDED`, alles nach `REFUNDED`) → **409**.
* `RECEIVED` ohne Zustand für jede Position → **400**, Feldfehler `conditions`.

## Tests

`ReturnApiTests` (customersCannotProcessReturns, unusedItemsComeBackAsReducedGoods,
secondReturnAddsToTheSameReducedProduct, usedItemsAreNotSoldAgain, receivingNeedsAConditionForEveryItem,
refundUsesOrderPricesAndDiscount, statusesFollowTheAllowedOrder).

---
status: "erfüllt"
prioritaet: "Must"
sprint: "Sprint 2"
quelle: "Vorgabe Sprint 2, B1, B2, B3"
adr: "ADR-0005, ADR-0006"
---

# Bestellen: nur was auf Lager ist, zu Preisen vom Shop

## Grobe Anforderung (Vorgabe)

> B1: Warenkorb + bestellen (keine echte Zahlung).
> B2: Nichts verkaufen, was nicht auf Lager ist.
> B3: Preise und Bestand bestimmt der Shop, nicht der Browser.

## UI/UX-Überlegung

* Im Warenkorb „Jetzt bestellen“ (angemeldet) bzw. „Anmelden und bestellen“ (Gast). Keine Zahlungsdaten, Hinweis „Keine echte Zahlung“.
* Nach dem Bestellen: Warenkorb wird geleert, Weiterleitung auf die Bestellung `/bestellungen/{id}` mit „Danke! Deine Bestellung ist eingegangen.“
* **Entscheidung und Begründung:** Der Browser schickt nur `productId` und `quantity`. Preis, Summe und Bestand rechnet ausschließlich das Backend. Damit zwei Kunden nicht gleichzeitig das letzte Stück kaufen, sperrt das Backend die Produkte während der Bestellung (ADR-0006).

## User Story

Als **Kunde** möchte ich meinen Warenkorb bestellen, damit ich die Produkte bekomme, und dabei sicher sein, dass nur verfügbare Ware zum angezeigten Preis bestellt wird.

## Akzeptanzkriterien

### Erwartetes Verhalten

* `POST /api/orders` mit `{ "items": [{ "productId": 5, "quantity": 2 }] }` → **201**, `Location: /api/orders/{id}`, Status „Eingegangen“.
* Der Bestand sinkt um die bestellte Menge.
* Einzelpreis und Summe in der Bestellung stammen aus der Datenbank. Schickt der Browser trotzdem einen Preis mit (`"price": 0.01`), wird er ignoriert.
* Der Preis wird in die Bestellung kopiert: Ändert der Admin später den Preis, bleibt die alte Bestellung gleich.

### Fehlerfälle

* Menge größer als Bestand oder ausverkauft → **409** „Nicht genug auf Lager …“ mit Liste `items` (Produkt, gewünscht, verfügbar). **Nichts** wird abgezogen, auch nicht bei den anderen Positionen (alles oder nichts).
* Unbekanntes Produkt → 409 mit „Unbekanntes Produkt“.
* Leerer Warenkorb, Menge 0 oder > 99 → **400**.
* Nicht angemeldet → **401**, Frontend schickt zur Anmeldung und danach zurück.
* Zwei Bestellungen gleichzeitig auf das letzte Stück → eine bekommt 201, die andere 409.

## Tests

`OrderApiTests` (Preis vom Server, Bestand sinkt, 409 ohne Teilabzug, letztes Stück, 400), `cart.spec.ts` (nur ID + Menge, 409-Anzeige).

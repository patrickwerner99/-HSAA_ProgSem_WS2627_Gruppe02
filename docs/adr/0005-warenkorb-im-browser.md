---
status: "angenommen"
date: "2026-10-30"
decision-makers: "Gruppe 02"
---

# Warenkorb im Browser, Bestellung rechnet das Backend

## Kontext und Problembeschreibung

Kunden sollen einen Warenkorb füllen und bestellen (B1). Wir haben entschieden, dass man sich erst
beim Bestellen anmeldet (0006). Wo liegt der Warenkorb, und wie stellen wir sicher, dass Preise und
Bestand vom Shop kommen und nicht vom Browser (B3)?

## Entscheidungstreiber

* Einkaufen ohne Konto, Anmeldung erst beim Bestellen
* Warenkorb soll Neuladen und das Anmelden überstehen
* Preise und Bestand dürfen nicht vom Browser manipulierbar sein (B3)
* Wenig zusätzlicher Code im Backend

## Betrachtete Optionen

* Warenkorb im localStorage des Browsers, nur Produkt-ID und Menge
* Warenkorb als Ressource im Backend (`/api/cart`), an Konto oder Sitzung gebunden
* Warenkorb nur im Speicher der Angular-App

## Entscheidungsergebnis

Gewählte Option: „localStorage, nur ID und Menge“, weil es ohne Konto funktioniert und das Backend
beim Bestellen ohnehin alles neu prüfen muss.

* Im Browser steht nur `[{ "productId": 5, "quantity": 2 }]`. Preise, Namen und Bestand lädt die
  Warenkorbseite jedes Mal frisch über `GET /api/products/{id}`.
* `POST /api/orders` bekommt nur IDs und Mengen. Das Backend lädt die Produkte, prüft den Bestand,
  nimmt die Preise aus der Datenbank und kopiert sie in die Bestellung. Zusätzliche Felder wie
  `price` werden ignoriert.

### Konsequenzen

* Gut, weil ein manipulierter Warenkorb nichts bringt: Ein gefälschter Preis wird nie verwendet.
* Gut, weil kein neuer Endpunkt und keine Tabelle für Warenkörbe nötig sind.
* Schlecht, weil der Warenkorb an Browser und Gerät hängt (kein Weiterkaufen am anderen Gerät).
  Das schränkt auch C2 ein: Der Link `/warenkorb` zeigt auf jedem Gerät dessen eigenen Warenkorb.
* Schlecht, weil der Bestand im Warenkorb veraltet sein kann. Abgefangen durch frisches Laden
  und die Prüfung beim Bestellen (409 mit Hinweis an der Position).

### Bestätigung

`OrderApiTests.orderUsesServerPriceAndReducesStock` schickt absichtlich einen Preis von 0,01 €
mit. `cart.spec.ts` prüft, dass das Frontend nur ID und Menge schickt.

## Vor- und Nachteile der Optionen

### Warenkorb im Backend

* Gut, weil geräteübergreifend und als Link teilbar.
* Schlecht, weil Gäste dann eine Sitzung bräuchten und beim Anmelden zwei Warenkörbe zusammengeführt werden müssten.

### Nur im Speicher der App

* Gut, weil am einfachsten.
* Schlecht, weil der Warenkorb beim Neuladen und beim Anmelden verloren geht.

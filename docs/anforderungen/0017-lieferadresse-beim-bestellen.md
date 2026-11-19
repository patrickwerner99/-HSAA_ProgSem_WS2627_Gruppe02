---
status: "erfüllt"
prioritaet: "Must"
sprint: "Sprint 3"
quelle: "Review-Feedback Sprint 2; Grundlage für Vorgabe Sprint 3, E2 und E3"
adr: "ADR-0009"
---

# Lieferadresse beim Bestellen

## Grobe Anforderung (Vorgabe)

> Review-Feedback Sprint 2: Beim Bestellen fehlt eine Lieferadresse, so kann der Shop nichts verschicken.
> Sprint 3, E3: Eine Bestellung zeigt auch später noch die Adresse und die Preise, mit denen sie aufgegeben wurde.

### Rahmenbedingungen und Qualität

* Geliefert wird nur innerhalb Deutschlands, deshalb kein Feld „Land“ und eine PLZ mit genau 5 Ziffern.
* Die Adresse prüft das Backend verbindlich (ADR-0003); das Formular prüft nur zur schnellen Rückmeldung.
* Bestellungen aus Sprint 2 haben keine Adresse und müssen trotzdem weiter angezeigt werden.

## UI/UX-Überlegung

* **Seiten und Dialoge:** Warenkorb (`/warenkorb`), Bestelldetails (`/bestellungen/{id}`) für Kunde und Admin.
* **Hauptaktion und Ablauf:** Angemeldete Kunden sehen im Warenkorb unter den Produkten den Block „Lieferadresse“ mit Name, Straße und Hausnummer, PLZ, Ort. Darunter „Jetzt bestellen“. Gäste melden sich wie bisher zuerst an.
* **Entscheidung und Begründung:** Die Adresse ist mit der Adresse der letzten Bestellung vorausgefüllt (sonst mit dem Namen aus dem Konto). Wer immer an dieselbe Adresse bestellt, muss nichts tippen. Mehrere gespeicherte Adressen kommen mit E2 (siehe Offene Punkte).
* **Mögliche Fehlerstellen:** Hausnummer vergessen, PLZ mit 4 Ziffern, Leerzeichen statt Ort.

## User Story

Als **Kunde** möchte ich beim Bestellen angeben, wohin die Ware geliefert wird, damit mein Paket ankommt, und die Adresse später in der Bestellung wiederfinden.

## Akzeptanzkriterien

### Erwartetes Verhalten

* Gegeben ein angemeldeter Kunde mit vollem Warenkorb, wenn er eine gültige Adresse eingibt und bestellt, dann antwortet `POST /api/orders` mit **201** und die Bestellung enthält `deliveryAddress` mit Name, Straße, PLZ und Ort.
* Gegeben eine Bestellung, wenn der Kunde oder der Admin sie öffnet, dann steht dort dieselbe Adresse wie beim Bestellen.
* Gegeben eine frühere Bestellung mit Adresse, wenn der Kunde den Warenkorb öffnet, dann ist das Formular mit dieser Adresse vorausgefüllt und der Hinweis „Aus deiner letzten Bestellung übernommen“ erscheint.
* Die Adresse wird in die Bestellung **kopiert** (wie die Preise, B3). Ändert der Kunde später seine Daten, bleibt die alte Bestellung unverändert (E3).

### Fehlerfälle

* Gegeben ein Bestellaufruf **ohne** `deliveryAddress`, dann **400** mit Feldfehler `deliveryAddress`, und es wird **kein** Bestand abgezogen.
* Gegeben eine PLZ wie `7343`, eine Straße ohne Hausnummer oder ein leerer Ort, dann **400** mit Feldfehlern `deliveryAddress.postalCode`, `deliveryAddress.street`, `deliveryAddress.city`. Im Warenkorb erscheinen die Meldungen direkt am Feld, darüber „Bitte prüfe die Lieferadresse.“
* Gegeben ein unvollständiges Formular, wenn der Kunde auf „Jetzt bestellen“ klickt, dann wird nichts geschickt, die Felder werden markiert und oben steht „Bitte gib eine vollständige Lieferadresse an.“
* Gegeben eine Bestellung aus Sprint 2 ohne Adresse, dann zeigt die Detailseite „Keine Adresse hinterlegt (ältere Bestellung)“ statt eines Fehlers.

## UX-Feinheiten (über die Vorgabe hinaus)

* Vorausgefüllte Adresse aus der letzten Bestellung, damit Stammkunden schneller bestellen.
* Browser-Autofill funktioniert (`autocomplete="street-address"`, `postal-code`, …).
* Hinweis „Wir liefern nur innerhalb Deutschlands.“ direkt unter dem Formular.

## Tests

`OrderApiTests` (deliveryAddressIsSavedAndShown, orderWithoutAddressIsRejectedAndNothingChanges, invalidAddressFieldsAreReported), `cart.spec.ts` (Adresse wird mitgeschickt, ohne Adresse keine Bestellung, Vorbelegung, Feldfehler vom Backend).

## Offene Punkte

* E2 (mehrere Adressen im Konto speichern und auswählen) baut darauf auf: Die gewählte Adresse wird genauso in die Bestellung kopiert.

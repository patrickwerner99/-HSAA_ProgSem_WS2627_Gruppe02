---
status: "angenommen"
date: "2026-11-19"
decision-makers: "Gruppe 02"
---

# Lieferadresse als Kopie in der Bestellung speichern

## Kontext und Problembeschreibung

Bestellungen brauchen eine Lieferadresse (0017). Sprint 3 fordert außerdem, dass Kunden mehrere Adressen
speichern (E2) und dass eine Bestellung auch später noch die Adresse zeigt, mit der sie aufgegeben wurde (E3).
Wo und wie speichern wir die Adresse einer Bestellung?

## Entscheidungstreiber

* Eine abgeschickte Bestellung darf sich nicht nachträglich ändern (wie bei den Preisen, B3).
* Später sollen gespeicherte Adressen im Konto dazukommen, ohne Bestellungen umzubauen.
* Ältere Bestellungen aus Sprint 2 ohne Adresse müssen weiter funktionieren.

## Betrachtete Optionen

* Adresse als Kopie in der Bestellung (eingebettete Spalten `delivery_*` in `shop_order`)
* Verweis von der Bestellung auf eine Adress-Tabelle (`address_id`)

## Entscheidungsergebnis

Gewählte Option: „Kopie in der Bestellung“, weil nur so eine Bestellung wirklich unveränderlich bleibt (E3).

* `DeliveryAddress` ist eine `@Embeddable`-Klasse; ihre Felder liegen als Spalten `delivery_name`, `delivery_street`,
  `delivery_postal_code`, `delivery_city` direkt in `shop_order`.
* Die Spalten dürfen leer sein, damit `ddl-auto=update` die Tabelle bei bestehenden Daten ergänzen kann und Bestellungen
  aus Sprint 2 weiter laden.
* Gespeicherte Adressen im Konto (E2) sind später eine eigene Tabelle. Beim Bestellen wird die gewählte Adresse
  genauso in die Bestellung **kopiert**.

### Konsequenzen

* Gut, weil Umzug, Tippfehler-Korrektur oder Löschen einer gespeicherten Adresse keine alte Bestellung verändern.
* Gut, weil Lesen einer Bestellung keinen zusätzlichen Join braucht.
* Schlecht, weil dieselbe Adresse mehrfach in der Datenbank steht. Bei unseren Datenmengen unerheblich.

### Bestätigung

`OrderApiTests.deliveryAddressIsSavedAndShown`, `orderWithoutAddressIsRejectedAndNothingChanges`.

## Vor- und Nachteile der Optionen

### Verweis auf eine Adress-Tabelle

* Gut, weil jede Adresse nur einmal gespeichert ist.
* Schlecht, weil eine geänderte oder gelöschte Adresse alte Bestellungen verfälscht oder kaputt macht. Man bräuchte zusätzlich „Adressen nie ändern, nur neue anlegen“, was komplizierter ist als die Kopie.

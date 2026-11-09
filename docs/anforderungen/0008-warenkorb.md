---
status: "erfüllt"
prioritaet: "Must"
sprint: "Sprint 2"
quelle: "Vorgabe Sprint 2, B1"
adr: "ADR-0005"
---

# Warenkorb

## Grobe Anforderung (Vorgabe)

> B1: Warenkorb + bestellen (keine echte Zahlung).

## UI/UX-Überlegung

* Auf der Detailseite: Menge mit −/+ wählen, „In den Warenkorb“. Rückmeldung unten mit Knopf „Zum Warenkorb“.
* In der Kopfzeile „Warenkorb“ mit Anzahl der Artikel.
* `/warenkorb`: Positionen mit Bild, Name, Einzelpreis, Menge (−/+), Zeilensumme, „Entfernen“; darunter die Summe.
* **Entscheidung und Begründung:** Der Warenkorb liegt im Browser (localStorage), nicht im Backend. So funktioniert er ohne Konto und übersteht Neuladen. Gespeichert werden nur Produkt-ID und Menge, Preise und Bestand lädt die Seite immer frisch (ADR-0005).

## User Story

Als **Kunde** möchte ich Produkte in einen Warenkorb legen und die Mengen anpassen, damit ich mehrere Produkte auf einmal bestellen kann.

## Akzeptanzkriterien

### Erwartetes Verhalten

* Gegeben ein Produkt ist auf Lager, wenn ich „In den Warenkorb“ klicke, dann erhöht sich die Zahl in der Kopfzeile.
* Gleiches Produkt zweimal hinzufügen → eine Position mit addierter Menge.
* Ich kann nicht mehr hinzufügen, als auf Lager ist (Knopf „+“ ist dann gesperrt), höchstens 99 Stück.
* Der Warenkorb bleibt nach Neuladen der Seite und nach dem Anmelden erhalten.
* Preise im Warenkorb sind immer die aktuellen Preise vom Shop.

### Fehlerfälle

* Produkt wurde inzwischen gelöscht → „Dieses Produkt gibt es nicht mehr.“, nur „Entfernen“ möglich.
* Bestand ist inzwischen kleiner als die Menge → „Nur noch N verfügbar.“ mit Knopf „Auf N reduzieren“; „Bestellen“ ist gesperrt, bis das passt.
* Ausverkaufte Produkte zeigen keinen „In den Warenkorb“-Knopf.
* Manipulierte Einträge im localStorage (negative Menge, erfundene Preise) bringen nichts: Preise werden nie aus dem Browser übernommen, ungültige Mengen lehnt das Backend beim Bestellen mit 400 ab. Ist der Speicher kaputt, startet der Warenkorb leer.

## Tests

`cart.service.spec.ts`, `cart.spec.ts`.

## Offene Punkte

* Der Warenkorb ist an Browser und Gerät gebunden. Wer am Handy etwas einlegt, sieht es am PC nicht. Für Sprint 2 bewusst so (ADR-0005).

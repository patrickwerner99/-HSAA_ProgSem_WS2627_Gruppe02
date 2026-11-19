---
status: "erfüllt"
prioritaet: "Should"
sprint: "Sprint 3"
quelle: "Review-Feedback Sprint 2, Erweiterung von 0004"
adr: "keine"
---

# Realistisches Sortiment als Beispieldaten

## Grobe Anforderung (Vorgabe)

> Review-Feedback Sprint 2: mehr Produktvielfalt, damit der Shop echter wirkt (Riegel, Getränke, Pulver in verschiedenen Größen).

## UI/UX-Überlegung

* Ein frischer Start (`docker compose up`) zeigt ein Sortiment, mit dem sich alle Filter sinnvoll vorführen lassen: jede Produktart, jede Bestandsstufe, mehrere Größen desselben Produkts.
* Jedes neue Produkt hat ein eigenes Bild im Stil der bisherigen (Riegel, Flasche, Gel-Beutel, Dose), damit die Liste einheitlich aussieht.

## User Story

Als **Team** möchten wir beim Start ein realistisches Sortiment haben, damit wir Suche, Filter, Sortierung und Bestellung im Review mit echten Fällen zeigen können.

## Akzeptanzkriterien

* Beim ersten Start werden **31 Produkte in 5 Kategorien** angelegt: Kreatin, Protein, Aminosäuren, Hyrox-Supplement und neu „Energie & Elektrolyte“.
* Alle fünf Produktarten kommen vor (Pulver, Riegel, Getränk, Kapseln & Tabletten, Gel).
* Mindestens ein Produkt gibt es in drei Größen (Whey Protein Schoko 500 g / 1 kg / 2,5 kg, Kreatin Neutral 250 g / 500 g / 1 kg).
* Es gibt Produkte mit jedem Bestandsstatus: verfügbar, „Nur noch N“ und ausverkauft.
* Wie in 0004 werden die Daten nur eingespielt, wenn die Datenbank leer ist. Wer schon Daten hat, sieht das neue Sortiment erst nach `docker compose down -v`.

### Fehlerfälle

* Fehlt ein Bild, zeigt die Seite den Platzhalter (D3).
* Schlägt das Einspielen fehl, startet der Shop trotzdem (wie 0004).

## Tests

`ProductApiTests` (31 Produkte, Seiten à 12, Sortierung nach Preis mit neuem günstigstem/teuerstem Produkt), `CategoryApiTests` (5 Kategorien, Protein mit 11 Produkten).

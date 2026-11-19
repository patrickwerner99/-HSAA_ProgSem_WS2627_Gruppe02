---
status: "erfüllt"
prioritaet: "Must"
sprint: "Sprint 3"
quelle: "Review-Feedback Sprint 2"
adr: "keine"
---

# Packungsgrößen

## Grobe Anforderung (Vorgabe)

> Review-Feedback Sprint 2: Der Shop soll echter wirken, z. B. Pulver und Getränke in verschiedenen Portions- bzw. Packungsgrößen.

## UI/UX-Überlegung

* **Seiten:** Produktliste (Kachel), Produktdetails, Warenkorb, Bestellung, Pflegeseite.
* **Entscheidung und Begründung:** Jede Packungsgröße ist ein **eigenes Produkt** mit eigenem Preis und Bestand (z. B. „Whey Protein Schoko“ 500 g, 1 kg, 2,5 kg). Das passt zu Sprint 1, wo schon jede Sorte ein eigenes Produkt ist, und Warenkorb, Bestand und Bestellung funktionieren ohne Änderung weiter. Die Alternative „Varianten unter einem Produkt“ hätte Warenkorb, Bestand und Bestellung komplett umgebaut.
* Die Größe ist ein freier Text bis 30 Zeichen („1 kg“, „330 ml“, „12 × 45 g“), weil die Einheiten je nach Produktart verschieden sind.
* Auf der Detailseite stehen unter dem Preis die anderen Größen desselben Produkts als Links („Auch erhältlich: 500 g · 19,90 €“).

## User Story

Als **Kunde** möchte ich sehen, wie groß eine Packung ist, und zwischen verschiedenen Größen wählen, damit ich die passende Menge zum passenden Preis kaufe.

## Akzeptanzkriterien

### Erwartetes Verhalten

* `GET /api/products` und `GET /api/products/{id}` liefern das Feld `size` (z. B. `"1 kg"`, bei Produkten ohne Größe `null`).
* Kachel und Detailseite zeigen die Größe unter dem Namen; Warenkorb und Bestellung zeigen „Name (Größe)“.
* Die Bestellposition übernimmt den Namen **mit** Größe, z. B. „Kreatin Neutral (500 g)“, damit eine Bestellung eindeutig bleibt, auch wenn das Produkt später geändert wird.
* Gegeben „Whey Protein Schoko“ in 500 g, 1 kg und 2,5 kg, wenn der Kunde eine Größe öffnet, dann stehen die beiden anderen als Links mit Preis darunter.
* Der Admin pflegt die Größe im Produktformular (optional) und sieht sie in der Produkttabelle.

### Fehlerfälle

* Größe länger als 30 Zeichen → **400** mit Feldfehler `size`, im Formular als Meldung am Feld.
* Lassen sich die anderen Größen nicht laden, fehlt nur der Hinweis „Auch erhältlich“; die Detailseite bleibt nutzbar.

## Tests

`ProductApiTests` (sameProductInSeveralSizes, tooLongSizeIsAFieldError, createReturns201AndLocation prüft `size`), `OrderApiTests` (Positionsname mit Größe).

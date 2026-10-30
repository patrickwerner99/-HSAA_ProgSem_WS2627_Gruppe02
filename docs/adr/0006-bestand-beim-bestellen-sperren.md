---
status: "angenommen"
date: "2026-10-30"
decision-makers: "Gruppe 02"
---

# Bestand beim Bestellen sperren (pessimistische Sperre)

## Kontext und Problembeschreibung

B2: Der Shop darf nichts verkaufen, was nicht auf Lager ist. Bestellen zwei Kunden gleichzeitig
das letzte Stück, könnten beide „Bestand 1“ lesen, beide bestellen und der Bestand landet bei −1.
Wie verhindern wir das?

## Entscheidungstreiber

* Bestand darf nie negativ werden, auch bei gleichzeitigen Bestellungen
* Eine Bestellung mit mehreren Positionen gilt ganz oder gar nicht
* Für das Team verständlich und im Review erklärbar

## Betrachtete Optionen

* Pessimistische Sperre: Produkte beim Bestellen mit `SELECT … FOR UPDATE` sperren
* Optimistische Sperre: Versionsspalte (`@Version`), bei Konflikt Fehler und neu versuchen
* Bedingtes Update: `UPDATE product SET stock = stock - ? WHERE id = ? AND stock >= ?`

## Entscheidungsergebnis

Gewählte Option: „Pessimistische Sperre“, weil sie am einfachsten zu erklären ist und
Bestellungen bei uns kurz sind.

`OrderService.place` läuft in einer Transaktion:

1. Mengen pro Produkt zusammenzählen.
2. Alle Produkte der Bestellung mit `findAllByIdForUpdate` laden. MariaDB sperrt diese Zeilen,
   eine zweite Bestellung auf dieselben Produkte wartet, bis die erste fertig ist.
3. Bestand aller Positionen prüfen. Fehlt etwas → 409 mit Liste, nichts wird geändert.
4. Bestand abziehen, Bestellung mit den Preisen aus der Datenbank speichern.

Stornieren gibt den Bestand auf dieselbe Weise zurück.

### Konsequenzen

* Gut, weil zwei gleichzeitige Bestellungen sauber nacheinander laufen; die zweite sieht den
  neuen Bestand und bekommt 409.
* Gut, weil „alles oder nichts“ über die Transaktion gilt.
* Schlecht, weil gleichzeitige Bestellungen auf dasselbe Produkt kurz warten. Bei unserer Last egal.
* Neutral: Die Sperre greift nur beim Bestellen, nicht beim normalen Anzeigen.

### Bestätigung

`OrderApiTests.orderingMoreThanStockIsRejectedAndNothingChanges` und `lastPiecesCanBeBoughtExactly`.

## Vor- und Nachteile der Optionen

### Optimistische Sperre

* Gut, weil niemand warten muss.
* Schlecht, weil bei einem Konflikt die ganze Bestellung wiederholt werden muss und das Admin-Formular dann auch Versionskonflikte behandeln müsste.

### Bedingtes Update

* Gut, weil sehr schnell.
* Schlecht, weil es pro Position ein eigenes SQL braucht und die Rückmeldung „wie viele sind noch da“ extra gelesen werden muss.

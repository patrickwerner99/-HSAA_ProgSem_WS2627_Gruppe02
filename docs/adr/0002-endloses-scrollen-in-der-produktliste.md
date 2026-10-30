---
status: "angenommen"
date: "2026-10-30"
decision-makers: "Gruppe 02"
---

# Endloses Scrollen in der Produktliste

## Kontext und Problembeschreibung

Die Produktliste soll einen schnellen Überblick geben (Anforderung 0001). Alle Produkte auf einmal
zu laden skaliert nicht, sobald das Sortiment wächst oder später mehrere Händler dazukommen. Die
Dozenten haben im Kickoff ausdrücklich gefragt: „Seiten oder endloses Scrollen? Was ist für Nutzer
besser, und warum?“ Wie teilen wir die Liste auf, und wie liefert die API die Daten?

## Entscheidungstreiber

* Stöbern soll einfach sein: ein Shop für Sport-Supplements lebt vom Entdecken, nicht vom gezielten Nachschlagen
* Funktioniert gleich gut auf dem Smartphone
* Die API liefert nie mehr Daten als nötig
* Nutzer dürfen ihre Position nicht verlieren, wenn sie ein Produkt öffnen und zurückkommen

## Betrachtete Optionen

* Endloses Scrollen (automatisches Nachladen)
* Klassische Seitennummern
* Button „Mehr laden“

## Entscheidungsergebnis

Gewählte Option: „Endloses Scrollen“, weil es zum Stöbern am besten passt und auf dem Smartphone
das gewohnte Verhalten ist. Die bekannten Nachteile fangen wir gezielt ab (siehe Konsequenzen).

Technisch: Die API liefert Produkte in Ladungen zu 12 Stück
(`GET /api/products?q=&categoryId=&page=0&size=12`) und sagt mit `last: true`, dass nichts mehr
kommt. Im Frontend beobachtet ein `IntersectionObserver` ein unsichtbares Element unter der Liste
und lädt die nächste Ladung, sobald es in Sichtweite kommt.

### Konsequenzen

* Gut, weil Nutzer ohne Klicks weiterstöbern und das Backend nur 12 Produkte pro Anfrage liefert.
* Gut, weil Suche und Kategorie in der URL stehen (`?q=whey&kategorie=2`) und damit teilbar sind.
* Schlecht, weil man beim Zurückkehren von der Detailseite oben landen würde. Abgefangen: Liste und Scroll-Position werden beim Zurück-Button wiederhergestellt (`ProductListStore`).
* Schlecht, weil ein Footer unerreichbar wird. Abgefangen: Navigation steht in der festen Kopfzeile.
* Schlecht, weil ein Fehler beim Nachladen leicht untergeht. Abgefangen: bereits geladene Produkte bleiben stehen, unten erscheint „Erneut versuchen“.
* Schlecht, weil man nicht direkt zu „Seite 5“ springen kann. Bei unserem Sortiment ersetzen Suche und Kategorie-Filter das.

### Bestätigung

Backend-Tests prüfen die Ladungen (12 von 13 Beispielprodukten, dann der Rest mit `last: true`).
Im Browser: ans Listenende scrollen lädt nach, „Alle Produkte angezeigt“ erscheint am Ende,
Zurück von der Detailseite landet an derselben Stelle.

## Vor- und Nachteile der Optionen

### Endloses Scrollen

* Gut, weil flüssiges Stöbern, besonders auf dem Smartphone.
* Schlecht, weil Position und Footer ohne Gegenmaßnahmen verloren gehen.

### Klassische Seitennummern

* Gut, weil Position und Footer erhalten bleiben und man gezielt springen kann.
* Schlecht, weil jeder Seitenwechsel ein Klick und ein Sprung nach oben ist; wirkt bei einem Shop zum Stöbern sperrig.

### Button „Mehr laden“

* Gut, weil Nutzer selbst entscheiden, wann nachgeladen wird; Footer bleibt erreichbar.
* Neutral, weil technisch fast identisch zum endlosen Scrollen (gleiche API).
* Schlecht, weil ein zusätzlicher Klick nötig ist.

## Weitere Informationen

Umsetzung: `ProductController#list`, `ProductService#search`, `frontend/src/app/pages/product-list/`.
Anforderung: `docs/anforderungen/0001-produktliste.md`.
Ein Wechsel auf „Mehr laden“ wäre nur eine Änderung im Frontend, die API bleibt gleich.

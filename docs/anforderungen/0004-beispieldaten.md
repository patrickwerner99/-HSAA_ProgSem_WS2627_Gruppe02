---
status: "erfüllt"
prioritaet: "Must"
sprint: "Sprint 1"
quelle: "Vorgabe Sprint 1 (Canvas)"
adr: "ADR-0001 (Datenbank)"
---

# Beispielprodukte beim Start

## Grobe Anforderung (Vorgabe)

> Als User des E-Commerce Hub möchte ich beim Starten des Shops **Beispielprodukte** sehen.

Aus der Vision: „Wer den Hub betreibt, kann sich darauf verlassen, dass das System auf jedem Rechner zuverlässig startet.“

### Rahmenbedingungen und Qualität

* Das ganze System startet mit `docker-compose up` ohne weitere Schritte (Vorgabe Rahmenbedingungen).
* Die Beispieldaten kommen aus dem Backend (nicht im Frontend fest eingebaut), damit sie über die Pflegeseite änderbar sind.

## UI/UX-Überlegung

* **Seiten und Dialoge:** Keine eigene Seite. Die Beispieldaten erscheinen in Produktliste, Detailseite und Pflegeseite.
* **Entscheidung und Begründung:** Beispieldaten werden **nur eingespielt, wenn die Datenbank leer ist**. So überschreibt ein Neustart keine Änderungen, die der Betreiber gemacht hat.
* **Inhalt:** Unser Hyrox-Sortiment, damit die Demo zum Shop passt:

| Kategorie | Produkte (je Sorte ein Eintrag) |
| --- | --- |
| Kreatin | Kreatin Neutral, Kreatin Zitrone, Kreatin Kirsche |
| Protein | Whey Protein Schoko, Vanille, Erdbeere, Cookies |
| Aminosäuren | EAA Mango, EAA Eistee Pfirsich, EAA Cola |
| Hyrox-Supplement | Hyrox-Supplement Orange, Beere, Zitrone-Limette |

  Das sind 13 Produkte: mehr als die 12 der ersten Ladung, so ist das endlose Scrollen in der Demo sichtbar. Mindestens ein Produkt ist ausverkauft und eines hat wenig Bestand, damit alle Bestandsstatus vorführbar sind.

## User Story

Als **Kunde** möchte ich beim ersten Start des Shops schon Beispielprodukte sehen, damit der Shop sofort benutzbar und vorführbar ist.

## Akzeptanzkriterien

### Erwartetes Verhalten

* Gegeben die Datenbank ist leer, wenn ich das System mit `docker-compose up` starte, dann zeigt die Produktliste die 13 Beispielprodukte in 4 Kategorien.
* Gegeben die Beispieldaten sind eingespielt, wenn ich die Produktliste öffne, dann ist jeder Bestandsstatus mindestens einmal vertreten („Auf Lager“, „Nur noch wenige verfügbar“, „Ausverkauft“).
* Gegeben ich habe über die Pflegeseite ein Produkt geändert, wenn ich das System neu starte, dann ist meine Änderung noch da und es wurden keine Beispielprodukte doppelt angelegt.
* Gegeben jedes Beispielprodukt, wenn es angezeigt wird, dann hat es ein Bild, einen Preis und eine Beschreibung.

### Fehlerfälle

* Gegeben das Einspielen der Beispieldaten schlägt fehl, wenn das Backend startet, dann steht der Grund im Log und das Backend startet trotzdem (leere Liste mit Hinweis „Noch keine Produkte vorhanden“ statt Absturz).
* Gegeben der Betreiber hat alle Produkte gelöscht, wenn der Shop neu startet, dann werden die Beispieldaten **nicht** automatisch neu eingespielt, sofern noch Kategorien existieren. Die Liste zeigt „Noch keine Produkte vorhanden“.

## UX-Feinheiten (über die Vorgabe hinaus)

* **Eigene Produktbilder** im Hyrox-Design (im Frontend unter `/images/` mitgeliefert), damit die Demo ohne Internetverbindung funktioniert.
* **Reset für Entwickler:** `docker-compose down -v` löscht die Daten, beim nächsten Start sind die Beispieldaten wieder da. Steht im README.

## Offene Punkte

* **Sorten als eigene Produkte:** In Sprint 1 ist jede Sorte ein eigenes Produkt. Ein echtes Varianten-Modell (ein Produkt, mehrere Sorten und Größen) ist für einen späteren Sprint vorgesehen.

---
status: "erfüllt"
prioritaet: "Must"
sprint: "Sprint 1"
quelle: "Vorgabe Sprint 1 (Canvas)"
adr: "ADR-0003 (Validierung im Backend)"
---

# Pflegeseite: Produkte und Kategorien anlegen, ändern, löschen

## Grobe Anforderung (Vorgabe)

> Als User des E-Commerce Hub möchte ich auf einer eigenen Seite Produkte und Kategorien
> **anlegen, ändern und löschen**. Das Ändern funktioniert für ein ganzes Produkt und für
> einzelne Felder.

Aus dem Kickoff: „Wie macht ihr es Nutzern leicht, Produkte fehlerfrei anzulegen und zu ändern?“

### Rahmenbedingungen und Qualität

* Eigene Seite `/pflege`, erreichbar über die Kopfzeile.
* Login und Rollen sind **nicht** Teil von Sprint 1 (siehe Offene Punkte).
* Fehlertoleranz: Ungültige Eingaben werden im Frontend **und** im Backend abgefangen (ADR-0003). Das Backend speichert nie ein ungültiges Produkt, auch nicht bei einem direkten API-Aufruf.

## UI/UX-Überlegung

* **Seiten und Dialoge:** `/pflege` mit zwei Reitern „Produkte“ und „Kategorien“. Jeder Reiter zeigt eine Tabelle mit Button „+ Neues Produkt“ bzw. „+ Neue Kategorie“. Anlegen und Bearbeiten im selben Formular-Dialog.
* **Hauptaktion und Ablauf:**
  * Ganzes Produkt ändern: Button „Bearbeiten“ → Formular mit allen Feldern → „Speichern“.
  * Einzelnes Feld ändern: **Preis und Bestand direkt in der Tabelle** anklicken, Wert ändern, Enter. Das sind die Felder, die sich im Alltag am häufigsten ändern.
  * Löschen: Button „Löschen“ → Bestätigungsdialog → „Löschen“.
* **Entscheidung und Begründung:** Direktbearbeitung in der Tabelle nur für Preis und Bestand, alles andere über das Formular. So bleibt die Tabelle übersichtlich und die häufigsten Änderungen gehen mit einem Klick.
* **Mögliche Fehlerstellen:** leere Pflichtfelder, negativer Preis oder Bestand, Komma statt Punkt beim Preis, versehentliches Löschen, doppelter Kategoriename, Kategorie mit Produkten löschen.

### Felder

| Feld | Pflicht | Regel |
| --- | --- | --- |
| Name (Produkt) | ja | 2–100 Zeichen |
| Kategorie | ja | Auswahl aus vorhandenen Kategorien |
| Preis | ja | größer 0, höchstens 2 Nachkommastellen, max. 9.999,99 € |
| Bestand | ja | ganze Zahl ≥ 0 |
| Beschreibung | nein | max. 1.000 Zeichen |
| Bild-URL | nein | leer oder gültige `http(s)`-Adresse bzw. Pfad `/images/…` |
| Name (Kategorie) | ja | 2–50 Zeichen, eindeutig (ohne Groß-/Kleinschreibung) |

## User Story

Als **Shop-Betreiber** möchte ich Produkte und Kategorien auf einer eigenen Seite anlegen, ändern und löschen, damit mein Angebot im Shop immer aktuell ist.

## Akzeptanzkriterien

### Erwartetes Verhalten

* Gegeben ich bin auf `/pflege`, wenn ich „Neues Produkt“ ausfülle und speichere, dann erscheint es sofort in der Tabelle und in der Produktliste des Shops, und ich sehe „Produkt gespeichert“.
* Gegeben ein Produkt existiert, wenn ich es über das Formular ändere und speichere, dann werden alle Felder übernommen (ganzes Produkt ändern).
* Gegeben ein Produkt existiert, wenn ich in der Tabelle nur den Bestand von 24 auf 10 ändere, dann wird **nur der Bestand** gespeichert, alle anderen Felder bleiben unverändert (einzelnes Feld ändern).
* Gegeben ich klicke bei einem Produkt auf „Löschen“, wenn ich im Dialog „Löschen“ bestätige, dann ist es aus Tabelle und Shop verschwunden. Bei „Abbrechen“ passiert nichts.
* Gegeben ich lege die Kategorie „Recovery“ an, wenn ich danach ein Produkt anlege, dann kann ich „Recovery“ als Kategorie auswählen.
* Gegeben eine Kategorie ist leer, wenn ich sie lösche und bestätige, dann ist sie entfernt.
* Gegeben ich tippe beim Preis „34,90“, wenn ich speichere, dann wird 34.90 gespeichert (Komma wird akzeptiert).

### Fehlerfälle

* Gegeben ein Pflichtfeld ist leer, wenn ich das Feld verlasse, dann steht direkt darunter in Rot, was fehlt (z. B. „Bitte einen Namen eingeben“), und „Speichern“ ist deaktiviert.
* Gegeben ich gebe einen Preis von 0 oder „-5“ ein, wenn ich das Feld verlasse, dann sehe ich „Der Preis muss größer als 0 sein“.
* Gegeben ich gebe beim Bestand „3,5“ oder „-1“ ein, dann sehe ich „Der Bestand muss eine ganze Zahl ab 0 sein“.
* Gegeben eine Kategorie „Protein“ existiert, wenn ich eine weitere Kategorie „protein“ anlege, dann sehe ich „Diese Kategorie gibt es schon“ und es wird nichts gespeichert.
* Gegeben eine Kategorie enthält noch Produkte, wenn ich sie löschen will, dann wird **nicht** gelöscht und ich sehe „Die Kategorie enthält noch 3 Produkte. Bitte die Produkte zuerst verschieben oder löschen.“ Das Frontend fragt in diesem Fall gar nicht erst nach, das Backend lehnt es zusätzlich mit 409 ab.
* Gegeben jemand umgeht das Formular und schickt ungültige Daten direkt an die API, wenn das Backend sie prüft, dann antwortet es mit Status 400 und einer Liste der fehlerhaften Felder, und nichts wird gespeichert.
* Gegeben das Speichern schlägt fehl (z. B. Backend nicht erreichbar), wenn ich auf „Speichern“ klicke, dann bleibt der Dialog offen, meine Eingaben bleiben erhalten und ich sehe „Speichern fehlgeschlagen. Bitte erneut versuchen.“

## UX-Feinheiten (über die Vorgabe hinaus)

* **Bild-Vorschau im Formular:** Sobald eine Bild-URL eingetragen ist, erscheint eine Vorschau. So sieht man sofort, ob das Bild funktioniert.
* **Bestätigung nennt den Namen:** „Möchtest du ‚Whey Protein Schoko‘ wirklich löschen?“, damit man nicht das falsche Produkt löscht.
* **Preis-Eingabe mit Komma** funktioniert, weil deutsche Nutzer so tippen.
* **Bestand 0 wird in der Tabelle rot markiert**, damit der Betreiber Nachschub-Bedarf sofort sieht.

## Offene Punkte

* **Zugriffsschutz:** In Sprint 1 kann jeder die Pflegeseite öffnen. Login und Rollen („Rollen“ steht als Kernwert in der Produktvision) folgen in einem späteren Sprint.

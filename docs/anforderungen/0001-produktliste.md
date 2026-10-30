---
status: "erfüllt"
prioritaet: "Must"
sprint: "Sprint 1"
quelle: "Vorgabe Sprint 1 (Canvas) + Eigene Idee"
adr: "ADR-0002 (endloses Scrollen)"
---

# Produktliste mit Suche und Kategorie-Filter

## Grobe Anforderung (Vorgabe)

> Als User des E-Commerce Hub möchte ich eine **Liste aller verfügbaren Produkte**
> mit Name, Preis und Bild sehen, damit ich schnell einen Überblick bekomme.

Aus dem Epic: „Shop mit **durchsuchbarem** Sortiment“.

Eigene Ergänzung des Teams: Kategorie-Filter und endloses Scrollen.

### Rahmenbedingungen und Qualität

* Frontend Angular (Angular Material), Daten über die REST-API des Spring-Boot-Backends.
* Antwortzeit: Die ersten Produkte erscheinen in unter 1 Sekunde nach dem Öffnen der Seite (lokales Docker-Setup).
* Bedienbarkeit: Die Liste funktioniert auch auf einem Smartphone-Bildschirm (ab 360 px Breite) ohne seitliches Scrollen.

## UI/UX-Überlegung

* **Seiten und Dialoge:** Startseite `/` = Produktliste mit Suchfeld und Kategorie-Auswahl oben, darunter Produktkacheln im Raster.
* **Hauptaktion und Ablauf:** Kacheln ansehen → Produkt anklicken → Detailseite (siehe [0002](0002-produktdetails-bestand.md)).
* **Entscheidung und Begründung:** **Endloses Scrollen** statt Seiten: Es werden 12 Produkte geladen, beim Erreichen des Listenendes automatisch die nächsten 12. Ein Shop lädt zum Stöbern ein, und Nutzer kennen das Verhalten von Instagram, Zalando und Co. Die Nachteile (Position geht verloren, Footer schwer erreichbar) fangen wir mit den UX-Feinheiten unten ab. Begründung im Detail: ADR-0002.
* **Filter:** Suchfeld für den Produktnamen **und** Auswahl der Kategorie. Mehr Filter bringen bei einem kleinen Sortiment keinen Mehrwert.
* **Mögliche Fehlerstellen:** Suche ohne Treffer, fehlendes oder kaputtes Produktbild, Backend nicht erreichbar, Nutzer verliert beim Zurückkehren von der Detailseite seine Scroll-Position.

## User Story

Als **Kunde** möchte ich alle Produkte mit Name, Preis und Bild sehen und nach Name oder Kategorie eingrenzen, damit ich schnell einen Überblick bekomme und finde, was ich suche.

## Akzeptanzkriterien

### Erwartetes Verhalten

* Gegeben es gibt Produkte, wenn ich die Startseite öffne, dann sehe ich die ersten 12 Produkte als Kacheln mit Bild, Name und Preis.
* Gegeben ein Produkt kostet 34.9, wenn es angezeigt wird, dann steht dort **„34,90 €“** (deutsches Format, immer zwei Nachkommastellen, Euro-Zeichen hinten).
* Gegeben mehr als 12 Produkte passen zur aktuellen Suche, wenn ich ans Ende der Liste scrolle, dann werden automatisch die nächsten 12 geladen und während des Ladens ist ein Ladeindikator sichtbar.
* Gegeben alle passenden Produkte sind geladen, wenn ich ans Ende scrolle, dann wird nichts mehr nachgeladen und ich sehe den Hinweis „Alle Produkte angezeigt“.
* Gegeben ich tippe „whey“ in das Suchfeld, wenn ich 300 ms aufhöre zu tippen, dann sehe ich nur Produkte, deren Name „whey“ enthält, unabhängig von Groß- und Kleinschreibung.
* Gegeben ich wähle die Kategorie „Protein“, wenn die Liste neu lädt, dann sehe ich nur Produkte dieser Kategorie. Suche und Kategorie lassen sich kombinieren.
* Gegeben ein Filter ist aktiv, wenn die Liste angezeigt wird, dann steht über der Liste die Trefferzahl, z. B. „4 Produkte“.

### Fehlerfälle

* Gegeben ein Produkt hat kein Bild oder die Bild-URL ist nicht erreichbar, wenn die Kachel angezeigt wird, dann erscheint ein neutrales Platzhalterbild statt eines kaputten Bild-Symbols.
* Gegeben meine Suche findet nichts, wenn die Liste leer ist, dann sehe ich „Keine Produkte gefunden“ und einen Button „Filter zurücksetzen“, der Suche und Kategorie leert.
* Gegeben das Backend ist nicht erreichbar, wenn ich die Startseite öffne, dann sehe ich „Die Produkte konnten nicht geladen werden.“ und einen Button „Erneut versuchen“, statt einer leeren Seite.
* Gegeben das Nachladen der nächsten 12 Produkte schlägt fehl, wenn ich ans Ende scrolle, dann bleiben die bereits geladenen Produkte sichtbar und unten erscheint „Erneut versuchen“.

## UX-Feinheiten (über die Vorgabe hinaus)

* **Suche und Kategorie stehen in der URL** (`/?q=whey&kategorie=2`): Man kann den Link teilen, und Neuladen oder Zurück behält den Filter.
* **Scroll-Position bleibt erhalten:** Wer von der Detailseite zurückkommt, landet wieder beim zuletzt angesehenen Produkt statt ganz oben (größter Nachteil von endlosem Scrollen).
* **Ausverkauft-Hinweis auf der Kachel:** Ausverkaufte Produkte bleiben sichtbar, tragen aber das Badge „Ausverkauft“, damit niemand umsonst klickt.
* **Kein Footer unter der Liste:** Weil endloses Scrollen einen Footer unerreichbar macht, stehen Links wie „Pflege“ oben in der Kopfzeile.

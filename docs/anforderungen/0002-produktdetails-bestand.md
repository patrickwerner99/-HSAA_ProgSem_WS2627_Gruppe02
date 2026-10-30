---
status: "erfüllt"
prioritaet: "Must"
sprint: "Sprint 1"
quelle: "Vorgabe Sprint 1 (Canvas)"
adr: "keine"
---

# Produktdetails mit Bestandsstatus

## Grobe Anforderung (Vorgabe)

> Als User des E-Commerce Hub möchte ich **Produktdetails und Bestandsdaten** prüfen,
> damit ich weiß, ob und wie viel von einem Produkt verfügbar ist.

### Rahmenbedingungen und Qualität

* Detailseite unter einer eigenen URL `/produkte/{id}`, damit sie verlinkbar ist.
* Der genaue Bestand wird im Backend gespeichert (ganze Zahl ≥ 0) und auf der Pflegeseite als Zahl angezeigt.

## UI/UX-Überlegung

* **Seiten und Dialoge:** Detailseite mit großem Bild links, rechts Name, Kategorie, Preis, Bestandsstatus und Beschreibung. Auf dem Smartphone untereinander.
* **Hauptaktion und Ablauf:** Klick auf eine Kachel in der Produktliste → Detailseite → „Zurück zur Übersicht“.
* **Entscheidung und Begründung:** Kunden sehen den Bestand als **Status** statt als genaue Zahl. Ein Status ist auf einen Blick verständlich, und Mitbewerber erfahren keine genauen Lagerzahlen. Ausnahme seit Sprint 2: Bei 1–5 Stück steht die genaue Zahl, weil Kunden dann wissen müssen, wie viel sie in den Warenkorb legen können. Die genaue Zahl sieht der Betreiber auf der Pflegeseite.
* **Mögliche Fehlerstellen:** Produkt existiert nicht (mehr), z. B. alter Link oder gerade gelöscht; ungültige ID in der URL; fehlendes Bild.

## User Story

Als **Kunde** möchte ich die Details eines Produkts und seine Verfügbarkeit sehen, damit ich weiß, ob ich es bekommen kann, bevor ich mich dafür entscheide.

## Akzeptanzkriterien

### Erwartetes Verhalten

* Gegeben ich bin in der Produktliste, wenn ich auf eine Kachel klicke, dann öffnet sich `/produkte/{id}` mit Bild, Name, Kategorie, Preis (Format „34,90 €“) und Beschreibung.
* Gegeben der Bestand ist größer als 5, wenn die Detailseite angezeigt wird, dann steht dort in Grün **„Auf Lager“**.
* Gegeben der Bestand liegt zwischen 1 und 5, wenn die Detailseite angezeigt wird, dann steht dort in Orange die genaue Zahl, z. B. **„Nur noch 3 verfügbar“** (seit Sprint 2, vorher „Nur noch wenige verfügbar“).
* Gegeben der Bestand ist 0, wenn die Detailseite angezeigt wird, dann steht dort in Rot **„Ausverkauft“**.
* Der Status ist nicht nur an der Farbe erkennbar, sondern immer auch als Text und mit Symbol (Barrierefreiheit).

### Fehlerfälle

* Gegeben das Produkt mit dieser ID existiert nicht, wenn ich `/produkte/999` öffne, dann sehe ich „Dieses Produkt gibt es nicht (mehr).“ und einen Link „Zur Produktübersicht“.
* Gegeben die ID in der URL ist keine Zahl (`/produkte/abc`), wenn die Seite lädt, dann sehe ich dieselbe Meldung statt eines technischen Fehlers.
* Gegeben das Backend ist nicht erreichbar, wenn die Detailseite lädt, dann sehe ich „Das Produkt konnte nicht geladen werden.“ und „Erneut versuchen“.
* Gegeben das Produkt hat kein oder ein kaputtes Bild, wenn die Seite angezeigt wird, dann erscheint das Platzhalterbild.

## UX-Feinheiten (über die Vorgabe hinaus)

* **Seitentitel im Browser-Tab** zeigt den Produktnamen, damit mehrere offene Tabs unterscheidbar sind.
* **„Zurück zur Übersicht“** führt zur Liste mit dem vorherigen Filter und der vorherigen Scroll-Position.

## Offene Punkte

* ~~Vorgabe sagt „ob und wie viel“ verfügbar ist.~~ **Geklärt in Sprint 2:** Bei 1–5 Stück zeigen wir die genaue Zahl („Nur noch 3 verfügbar“), sonst den Status. Mit dem Warenkorb (0008) braucht der Kunde die Zahl, damit er weiß, wie viel er bestellen kann.

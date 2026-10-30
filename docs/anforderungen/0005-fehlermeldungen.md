---
status: "erfüllt"
prioritaet: "Must"
sprint: "Sprint 1"
quelle: "Vorgabe Sprint 1 (Canvas)"
adr: "ADR-0003 (Validierung im Backend)"
---

# Sinnvolle Fehlermeldungen

## Grobe Anforderung (Vorgabe)

> Als User des E-Commerce Hub möchte ich **sinnvolle Fehlermeldungen** bei falschen
> Eingaben oder Fehlern im System angezeigt bekommen.

Aus dem Kickoff: „Ihr denkt wie UI/UX-Experten: Nutzer machen Fehler, ihr fangt sie ab.“

### Rahmenbedingungen und Qualität

* Alle Meldungen sind auf Deutsch, in ganzen Sätzen und sagen, **was passiert ist und was der Nutzer jetzt tun kann**.
* Der Nutzer sieht nie technische Details (Stacktrace, HTTP-Code, „null“, „undefined“).
* Das Backend liefert Fehler immer im selben JSON-Format (Problem Details, RFC 9457), damit das Frontend sie einheitlich anzeigen kann.

## UI/UX-Überlegung

* **Drei Arten von Fehlern, drei Darstellungen:**
  * **Falsche Eingabe** → rote Meldung direkt unter dem betroffenen Feld, sobald man das Feld verlässt.
  * **Aktion fehlgeschlagen** (z. B. Speichern) → Hinweisleiste unten (Snackbar), Eingaben bleiben erhalten.
  * **Seite kann nicht laden** → Meldung an der Stelle des Inhalts mit Button „Erneut versuchen“.
* **Entscheidung und Begründung:** Prüfung im Frontend für schnelle Rückmeldung, Prüfung im Backend als verbindliche Regel (ADR-0003).
* **Mögliche Fehlerstellen:** siehe Tabelle.

### Fehlerkatalog

| Situation | HTTP | Meldung für den Nutzer |
| --- | --- | --- |
| Pflichtfeld leer | 400 | „Bitte einen Namen eingeben.“ (je Feld) |
| Preis ≤ 0 oder ungültig | 400 | „Der Preis muss größer als 0 sein.“ |
| Bestand negativ oder keine ganze Zahl | 400 | „Der Bestand muss eine ganze Zahl ab 0 sein.“ |
| Ungültige Bild-URL | 400 | „Bitte eine gültige Bild-Adresse (http/https) eingeben oder das Feld leer lassen.“ |
| Kategorie doppelt | 409 | „Diese Kategorie gibt es schon.“ |
| Kategorie enthält Produkte | 409 | „Die Kategorie enthält noch N Produkte. Bitte die Produkte zuerst verschieben oder löschen.“ |
| Produkt/Kategorie nicht gefunden | 404 | „Dieses Produkt gibt es nicht (mehr).“ |
| Backend nicht erreichbar | – | „Der Shop ist gerade nicht erreichbar. Bitte versuche es gleich noch einmal.“ |
| Unerwarteter Serverfehler | 500 | „Da ist etwas schiefgelaufen. Bitte versuche es erneut.“ |

## User Story

Als **Kunde oder Shop-Betreiber** möchte ich bei falschen Eingaben oder Systemfehlern eine verständliche Meldung bekommen, damit ich weiß, was passiert ist und wie ich weiterkomme.

## Akzeptanzkriterien

### Erwartetes Verhalten

* Gegeben ich fülle ein Formular falsch aus, wenn ich das Feld verlasse, dann steht die Meldung aus dem Fehlerkatalog direkt unter diesem Feld.
* Gegeben das Backend lehnt eine Eingabe ab (400), wenn die Antwort ankommt, dann erscheint die Meldung des Backends am richtigen Feld im Formular.
* Gegeben das Backend antwortet mit einem Fehler, wenn ich die Antwort ansehe, dann enthält sie immer `status`, `title`, `detail` und bei Eingabefehlern eine Liste `errors` mit Feldname und Meldung.
* Gegeben eine Fehlermeldung wird angezeigt, wenn ich sie lese, dann enthält sie keinen technischen Text (keine Exception-Namen, keine Stacktraces, kein „undefined“).

### Fehlerfälle

* Gegeben das Backend ist gestoppt, wenn ich irgendeine Seite öffne oder etwas speichere, dann sehe ich „Der Shop ist gerade nicht erreichbar …“ und kann es ohne Neuladen der Seite erneut versuchen.
* Gegeben im Backend tritt ein unerwarteter Fehler auf, wenn die Antwort ankommt, dann sieht der Nutzer die allgemeine Meldung, und der technische Grund steht nur im Backend-Log.
* Gegeben ich rufe eine Adresse auf, die es im Shop nicht gibt (`/gibtsnicht`), wenn die Seite lädt, dann sehe ich „Diese Seite gibt es nicht.“ mit Link zur Startseite.

## UX-Feinheiten (über die Vorgabe hinaus)

* **Meldungen erst nach Verlassen des Feldes**, nicht schon beim ersten Tastendruck. Rot anzumeckern, während man noch tippt, nervt.
* **Erfolgsmeldungen** („Produkt gespeichert“, „Kategorie gelöscht“) erscheinen genauso einheitlich wie Fehler, damit der Nutzer immer weiß, ob seine Aktion geklappt hat.

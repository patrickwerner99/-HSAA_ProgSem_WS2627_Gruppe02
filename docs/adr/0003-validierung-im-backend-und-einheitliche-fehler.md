---
status: "angenommen"
date: "2026-10-30"
decision-makers: "Gruppe 02"
---

# Validierung im Backend verbindlich, einheitliches Fehlerformat

## Kontext und Problembeschreibung

Auf der Pflegeseite werden Produkte und Kategorien angelegt und geändert (0003), und Nutzer sollen
bei falschen Eingaben sinnvolle Meldungen sehen (0005). Wo prüfen wir die Eingaben, und in welchem
Format meldet das Backend Fehler, damit das Frontend sie einheitlich anzeigen kann?

## Entscheidungstreiber

* Es darf nie ein ungültiges Produkt gespeichert werden, auch nicht über einen direkten API-Aufruf
* Schnelle Rückmeldung direkt am Feld, ohne auf den Server zu warten
* Nutzer sehen nie technische Details (Stacktrace, Exception-Namen)
* Ein Format für alle Fehler, damit das Frontend nicht pro Fall eigene Logik braucht

## Betrachtete Optionen

* Prüfung im Frontend und im Backend, Backend ist verbindlich; Fehler als Problem Details (RFC 9457)
* Nur im Frontend prüfen
* Nur im Backend prüfen

## Entscheidungsergebnis

Gewählte Option: „Prüfung in Frontend und Backend, Backend verbindlich, Problem Details“, weil
nur so beide Ziele gleichzeitig erreicht werden: sofortige Rückmeldung im Formular und Sicherheit,
dass die Datenbank nur gültige Daten enthält.

* **Backend:** Bean Validation (`@NotBlank`, `@DecimalMin`, `@Digits`, …) auf `ProductRequest`,
  `ProductPatch` und `CategoryRequest`, deutsche Meldungen direkt an den Annotationen.
  Ein `@RestControllerAdvice` (`GlobalExceptionHandler`) übersetzt jeden Fehler in Problem Details:
  `{ "status": 400, "title": "Ungültige Eingabe", "detail": "…", "errors": [{ "field": "price", "message": "…" }] }`.
  Geschäftsregeln (doppelte Kategorie, Kategorie mit Produkten löschen) liefern 409, Unbekanntes 404,
  alles Unerwartete 500 mit allgemeiner Meldung; die Details stehen nur im Log.
* **Frontend:** dieselben Regeln als Validatoren im Formular; Fehler vom Backend werden am
  passenden Feld angezeigt (`fieldErrors`), alle anderen über `userMessage` übersetzt.

### Konsequenzen

* Gut, weil die Datenbank auch bei direkten API-Aufrufen sauber bleibt.
* Gut, weil Nutzer Fehler sofort beim Verlassen eines Feldes sehen.
* Gut, weil das Fehlerformat ein Standard ist (Spring bringt `ProblemDetail` mit).
* Schlecht, weil Regeln an zwei Stellen stehen und bei Änderungen beide angepasst werden müssen. Die Meldungstexte sind deshalb bewusst identisch, und die Backend-Tests prüfen die Regeln.
* Neutral: Der Bestand wird im Backend als `BigDecimal` angenommen, damit „3.5“ abgelehnt statt still auf 3 abgerundet wird.

### Bestätigung

`ProductApiTests` und `CategoryApiTests` schicken absichtlich ungültige Daten und prüfen Status,
Feldnamen und Meldungstexte. Frontend-Tests prüfen die Übersetzung der Fehler (`errors.spec.ts`).

## Vor- und Nachteile der Optionen

### Frontend und Backend, Backend verbindlich

* Gut, weil schnell für Nutzer und sicher für die Daten.
* Schlecht, weil doppelte Regeln gepflegt werden müssen.

### Nur im Frontend prüfen

* Gut, weil wenig Aufwand.
* Schlecht, weil jeder direkte API-Aufruf ungültige Daten speichern kann.

### Nur im Backend prüfen

* Gut, weil Regeln nur an einer Stelle stehen.
* Schlecht, weil Nutzer jede falsche Eingabe erst nach dem Absenden sehen.

## Weitere Informationen

Fehlerkatalog: `docs/anforderungen/0005-fehlermeldungen.md`.
Umsetzung: `backend/…/common/GlobalExceptionHandler.java`, `frontend/src/app/shared/errors.ts`,
`frontend/src/app/pages/admin/validators.ts`.

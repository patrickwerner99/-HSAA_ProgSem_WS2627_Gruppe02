---
status: "erfüllt"
prioritaet: "Must"
sprint: "Sprint 3"
quelle: "Sprint 3, E1"
adr: "ADR-0003, ADR-0004"
---

# Eigene Kontodaten ändern

## Grobe Anforderung (Vorgabe)

> E1: Kunden können ihre eigenen Kontodaten ändern.

## UI/UX-Überlegung

* **Seite:** „Mein Konto“ (`/konto`), in der Navigation nur sichtbar, wenn man angemeldet ist.
* Drei getrennte Bereiche: **Name & E-Mail**, **Passwort**, **Adressen** (0022). Jeder Bereich hat seinen eigenen
  Speichern-Knopf, damit eine Änderung am Namen nicht versehentlich das Passwort mitschickt.
* **Entscheidung:** Für eine neue E-Mail-Adresse muss das aktuelle Passwort eingegeben werden. Die E-Mail ist der
  Anmeldename; wer kurz an einem fremden, angemeldeten Rechner sitzt, soll das Konto nicht übernehmen können.
  Nur den Namen zu ändern geht ohne Passwort.
* Nach dem Speichern bleibt man angemeldet, auch wenn sich die E-Mail geändert hat.

## User Story

Als **Kunde** möchte ich Name, E-Mail und Passwort selbst ändern, damit meine Daten stimmen, ohne dass ich ein neues
Konto anlegen muss.

## Akzeptanzkriterien

### Erwartetes Verhalten

* `GET /api/account` liefert die eigenen Daten (ohne Passwort).
* `PATCH /api/account` mit `name` ändert nur den Namen → 200.
* `PATCH /api/account` mit `email` und `currentPassword` ändert die E-Mail → 200; die Sitzung bleibt gültig.
* `PUT /api/account/password` mit `currentPassword` und `newPassword` (mind. 8 Zeichen) → 204.
  Danach klappt die Anmeldung nur noch mit dem neuen Passwort.
* Jeder sieht und ändert nur sein eigenes Konto; eine ID in der URL gibt es deshalb nicht.

### Fehlerfälle

* Nicht angemeldet → **401**.
* Neue E-Mail ohne oder mit falschem aktuellem Passwort → **400**, Feldfehler `currentPassword`.
* E-Mail gehört schon einem anderen Konto → **409**.
* Ungültige E-Mail, leerer Name, neues Passwort zu kurz → **400** mit Feldfehler.
* Falsches altes Passwort beim Passwortwechsel → **400**, Feldfehler `currentPassword`.

## Tests

`AccountApiTests` (accountRequiresLogin, customerChangesName, changingEmailNeedsTheCurrentPassword,
emailOfAnotherAccountIsAConflict, passwordChangeChecksTheOldPassword, tooShortNewPasswordIsRejected).

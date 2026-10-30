---
status: "erfüllt"
prioritaet: "Must"
sprint: "Sprint 2"
quelle: "Vorgabe Sprint 2, N1, N2"
adr: "ADR-0003"
---

# REST-API nach den Regeln aus Vorlesung 1, mit .http-Datei

## Grobe Anforderung (Vorgabe)

> N1: API folgt den Regeln aus Vorlesung 1 (Ressourcen, Methoden, Statuscodes, Fehlerformat, Seiten).
> N2: `.http`-Datei mit jedem Endpunkt, mit Erfolgs- und Fehlerfall und erwartetem Statuscode.

## Umsetzung

* **Ressourcen** als Nomen im Plural: `/api/products`, `/api/categories`, `/api/orders`, `/api/images`. Unterressource `/api/products/{id}/image`.
* **Methoden:** GET lesen, POST anlegen (201 + `Location`), PUT ganz ersetzen, PATCH Teile ändern (auch Bestellstatus), DELETE löschen (204).
* **Statuscodes:** 200, 201, 204, 400 (ungültig), 401 (nicht angemeldet), 403 (keine Rechte), 404 (gibt es nicht), 409 (Konflikt: doppelt, Bestand, Statusfolge), 413 (Bild zu groß), 500 (unerwartet).
* **Fehlerformat:** überall Problem Details (RFC 9457), `application/problem+json`, mit `errors` bei Feldfehlern (ADR-0003). Auch 401/403 aus Spring Security.
* **Seiten:** Listen liefern `{ items, page, size, totalItems, last }`, Parameter `page` (ab 0) und `size` (begrenzt).
* **Anmeldung** ist kein Ressourcen-CRUD, sondern Aktion: `/api/auth/register|login|logout|me`.

## Akzeptanzkriterien

* `api.http` im Hauptordner enthält **jeden** Endpunkt mit mindestens einem Erfolgs- und einem Fehlerfall.
* Bei jedem Aufruf steht der erwartete Statuscode, und ein Test-Skript prüft ihn (IntelliJ HTTP Client: „Run all“ zeigt grün/rot).
* Passwörter stehen nicht in der Datei, sondern in `http-client.private.env.json` (nicht im Repo, Q3).

## Offene Punkte

* Die Folien zu Vorlesung 1 noch einmal mit der Tabelle oben abgleichen (z. B. ob Englisch oder Deutsch für Pfade gewünscht ist).

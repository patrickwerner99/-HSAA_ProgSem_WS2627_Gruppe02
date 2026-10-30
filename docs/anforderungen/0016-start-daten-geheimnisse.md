---
status: "erfüllt"
prioritaet: "Must"
sprint: "Sprint 2"
quelle: "Vorgabe Sprint 2, Q1, Q2, Q3"
adr: "ADR-0001, ADR-0007, ADR-0008"
---

# Start mit einem Befehl, Daten bleiben, keine Geheimnisse im Repo

## Grobe Anforderung (Vorgabe)

> Q1: Frischer Klon startet mit `docker compose up`, mit Beispieldaten und Admin-Zugang laut README; Daten überstehen Neustart.
> Q2: Wichtigste Akzeptanzkriterien durch automatisierte Tests abgesichert.
> Q3: Keine Geheimnisse (Passwörter, API-Keys) im Repo.

## Akzeptanzkriterien

* `git clone` + `docker compose up --build` startet Datenbank, Backend und Frontend ohne weitere Schritte.
* Beim ersten Start gibt es 4 Kategorien, 13 Produkte und einen Admin. Wie man an das Admin-Passwort kommt, steht im README (ADR-0008).
* Bestellungen, Konten, Produkte und hochgeladene Bilder überstehen `docker compose down` + `up` (Volumes `db-data` und `images`). Erst `down -v` setzt alles zurück.
* `mvn test` (Backend) und `npm test` (Frontend) laufen grün; jede Story nennt ihre Tests.
* Im Repo steht kein echtes Passwort und kein API-Key. `.env` und `*.private.env.json` sind in `.gitignore`.

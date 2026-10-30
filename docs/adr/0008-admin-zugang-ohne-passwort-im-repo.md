---
status: "angenommen"
date: "2026-10-30"
decision-makers: "Gruppe 02"
---

# Admin-Zugang ohne Passwort im Repo

## Kontext und Problembeschreibung

Q1 verlangt, dass ein frischer Klon mit `docker compose up` startet, „mit Admin-Zugang laut README“.
Q3 verlangt „keine Geheimnisse im Repo“. Ein festes Admin-Passwort im README oder in
`docker-compose.yml` würde Q3 verletzen. Wie kommt der Prüfer trotzdem an den Admin-Zugang?

## Entscheidungstreiber

* Start ohne Zusatzschritte (Q1)
* Kein echtes Passwort und kein Schlüssel im Repo (Q3)
* Wer den Shop betreibt, soll ein eigenes Passwort setzen können

## Betrachtete Optionen

* Zufälliges Passwort beim ersten Start, steht einmal im Backend-Log; optional eigenes über `ADMIN_PASSWORD`
* Festes Passwort im README
* Pflicht-Datei `.env`, die man vor dem Start anlegen muss

## Entscheidungsergebnis

Gewählte Option: „Zufälliges Passwort im Log, optional `ADMIN_PASSWORD`“.

* Gibt es noch keinen Admin, legt `AdminAccountInitializer` beim Start einen an
  (E-Mail `admin@hyrox-shop.local`, änderbar über `ADMIN_EMAIL`).
* Ist `ADMIN_PASSWORD` gesetzt (z. B. in einer `.env`-Datei neben `docker-compose.yml`, die per
  `.gitignore` nicht ins Repo kommt), wird dieses Passwort verwendet.
* Sonst wird ein zufälliges Passwort erzeugt und **einmal** gut sichtbar ins Log geschrieben.
  Das README erklärt, wie man es findet: `docker compose logs backend | Select-String Admin`.
* Das Passwort wird nur als BCrypt-Hash gespeichert.
* Für MariaDB setzen wir `MARIADB_RANDOM_ROOT_PASSWORD`, das root-Passwort steht also auch nicht im Repo.

### Konsequenzen

* Gut, weil jede Installation ein anderes Admin-Passwort hat.
* Gut, weil `docker compose up` ohne Vorbereitung funktioniert.
* Schlecht, weil man einmal ins Log schauen muss. Wird das Log gelöscht, hilft `docker compose down -v` (setzt alles zurück) oder `ADMIN_PASSWORD` setzen und den Admin in der Datenbank löschen.
* Neutral: Das Passwort des Datenbank-Nutzers `shop` steht weiterhin in `docker-compose.yml`.
  Die Datenbank ist nur im internen Docker-Netz erreichbar (kein Port nach außen), das Passwort ist
  also kein Geheimnis, mit dem man von außen etwas anfangen kann. Wer es ändern will, setzt `DB_PASSWORD`.
  Ob das den Dozenten reicht, klären wir im Review.

### Bestätigung

`AuthApiTests.adminFromStartupCanLogIn` (Testprofil setzt das Passwort per Property).
Prüfen vor der Abgabe: `git grep -i passw` zeigt keine echten Passwörter.

## Vor- und Nachteile der Optionen

### Festes Passwort im README

* Gut, weil am bequemsten für Prüfer.
* Schlecht, weil es Q3 direkt verletzt und jede Installation dasselbe Passwort hätte.

### Pflicht-Datei `.env`

* Gut, weil sauber.
* Schlecht, weil `docker compose up` ohne die Datei scheitert (Q1).

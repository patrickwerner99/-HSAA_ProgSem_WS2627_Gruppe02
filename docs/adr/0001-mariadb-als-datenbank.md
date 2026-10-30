---
status: "angenommen"
date: "2026-10-30"
decision-makers: "Gruppe 02"
---

# MariaDB als Datenbank

## Kontext und Problembeschreibung

Der E-Commerce-Hub muss Produkte, Kategorien und Bestände dauerhaft speichern, und Änderungen
aus der Pflegeseite müssen einen Neustart überleben. Die Datenbank ist laut Vorgabe frei wählbar
(„H2 bleibt eine Option“). Welche Datenbank nehmen wir, und wie bleibt dabei der Start mit einem
einzigen `docker-compose up` erhalten?

## Entscheidungstreiber

* Relationale Daten mit festen Beziehungen (Produkt → Kategorie) und später Transaktionen (Warenkorb, Bestellung, Bestand)
* Vision: „Das System startet auf jedem Rechner zuverlässig“, also bei allen im Team gleich, ohne lokale Installation
* Daten müssen Neustarts überleben (Anforderung 0004)
* Im Sprint Review 1 haben wir MariaDB als Datenbank vorgestellt und begründet
* Der Build (inkl. Tests) soll ohne laufende Datenbank funktionieren

## Betrachtete Optionen

* MariaDB als eigener Docker-Container
* H2 eingebettet im Backend (Datei-Modus)
* PostgreSQL als eigener Docker-Container

## Entscheidungsergebnis

Gewählte Option: „MariaDB als eigener Docker-Container“, weil sie alle Treiber erfüllt, einer
echten Produktivumgebung entspricht und zu dem passt, was wir im Review 1 vorgestellt haben.
Für die automatischen Tests nutzen wir H2 im Arbeitsspeicher (MariaDB-Kompatibilitätsmodus),
damit `mvn package` im Docker-Build keine laufende Datenbank braucht.

### Konsequenzen

* Gut, weil Daten im Docker-Volume `db-data` liegen und Neustarts überleben; `docker-compose down -v` setzt bewusst alles zurück.
* Gut, weil jedes Teammitglied dieselbe MariaDB-Version (11.4) bekommt, ohne etwas zu installieren.
* Gut, weil spätere Anforderungen (Warenkorb, Rabatte, Rollen) Transaktionen und Beziehungen brauchen.
* Schlecht, weil ein dritter Container dazukommt und das Backend erst startet, wenn die Datenbank bereit ist (Healthcheck).
* Schlecht, weil Tests gegen H2 statt gegen MariaDB laufen. Unterschiede im SQL-Dialekt fallen erst beim Start in Docker auf.
* Schlecht, weil das Schema in Sprint 1 per `ddl-auto=update` entsteht. Für spätere Sprints planen wir versionierte Migrationen (Flyway), dann mit eigenem ADR.

### Bestätigung

`docker-compose up -d --build` startet `db`, `backend` und `frontend`; `docker-compose ps` zeigt alle
drei als `healthy` bzw. `running`. Nach einem Neustart sind Änderungen aus der Pflegeseite noch da.

## Vor- und Nachteile der Optionen

### MariaDB als eigener Docker-Container

* Gut, weil verbreitet, relational, ACID-konform und frei verfügbar.
* Gut, weil das offizielle Image einen Healthcheck mitbringt (`healthcheck.sh`).
* Schlecht, weil ein zusätzlicher Container gepflegt werden muss.

### H2 eingebettet im Backend

* Gut, weil kein extra Container nötig ist und der Start am einfachsten ist.
* Neutral, weil die Daten im Datei-Modus mit einem Volume ebenfalls erhalten bleiben.
* Schlecht, weil H2 kaum in echten Produktivsystemen eingesetzt wird und wir spätestens beim Marktplatz wechseln müssten.

### PostgreSQL als eigener Docker-Container

* Gut, weil sehr mächtig (z. B. JSONB) und ebenso verbreitet.
* Neutral, weil der Aufwand gleich wäre wie bei MariaDB.
* Schlecht, weil wir im Review MariaDB begründet haben und ein Wechsel ohne neuen Grund keinen Mehrwert bringt.

## Weitere Informationen

Konfiguration: `docker-compose.yml` (Service `db`), `backend/src/main/resources/application.properties`,
Testkonfiguration in `backend/src/test/resources/application.properties`.
Erneut prüfen, wenn Bestellungen und Migrationen dazukommen.

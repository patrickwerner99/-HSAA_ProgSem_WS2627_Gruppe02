---
status: "angenommen"
date: "2026-10-30"
decision-makers: "Gruppe 02"
---

# Anmeldung mit Sitzungs-Cookie (Spring Security), Rollen KUNDE und ADMIN

## Kontext und Problembeschreibung

Ab Sprint 2 haben Kunden ein Konto (A1), und nur Admins dürfen Daten ändern, auch beim direkten
Aufruf der API (A2). Wie meldet sich jemand an, und wie erkennt das Backend bei jedem Aufruf,
wer er ist und was er darf?

## Entscheidungstreiber

* Die Sperre muss im Backend sitzen, nicht nur in der Oberfläche
* Passwörter nie im Klartext speichern
* Einfach genug, dass jedes Teammitglied es im Review erklären kann
* Frontend und Backend laufen unter derselben Adresse (nginx leitet `/api` weiter)

## Betrachtete Optionen

* Sitzung mit Cookie (Spring Security, `HttpSession`)
* JWT im `Authorization`-Header
* HTTP Basic bei jedem Aufruf

## Entscheidungsergebnis

Gewählte Option: „Sitzung mit Cookie“, weil Browser und API unter derselben Adresse laufen und
Spring Security das ohne zusätzliche Bibliothek kann.

* `POST /api/auth/login` prüft E-Mail und Passwort (BCrypt) und legt eine neue Sitzung an.
  Der Browser bekommt das Cookie `JSESSIONID` mit `HttpOnly` (kein JavaScript-Zugriff) und
  `SameSite=Strict` (wird bei Aufrufen von fremden Seiten nicht mitgeschickt).
* `SecurityConfig` legt fest, wer was darf: Lesen von Produkten, Kategorien und Bildern für alle,
  Ändern nur `ADMIN`, Bestellungen nur angemeldet. Was ein Kunde bei Bestellungen sehen darf
  (nur eigene), prüft zusätzlich `OrderService`.
* 401 und 403 kommen im selben Problem-Details-Format wie alle anderen Fehler (ADR-0003).
* Das Frontend fragt beim Start `GET /api/auth/me` und blendet danach Menüpunkte ein oder aus.
  Das ist nur Komfort; entschieden wird im Backend.

### Konsequenzen

* Gut, weil das Token nie in JavaScript oder im localStorage liegt (schützt vor Diebstahl über XSS).
* Gut, weil Abmelden sofort wirkt (Sitzung wird im Backend gelöscht), anders als bei JWT.
* Gut, weil nach dem Login immer eine neue Sitzung angelegt wird (Schutz vor Sitzungs-Fixierung).
* Schlecht, weil Sitzungen im Speicher des Backends liegen: Nach einem Neustart des Backends muss
  man sich neu anmelden. Für unser Projekt mit einem Backend-Container unkritisch.
* Neutral: CSRF-Token sind abgeschaltet. Den Schutz übernimmt `SameSite=Strict`: Der Browser
  schickt unser Cookie nicht mit, wenn eine fremde Seite einen Aufruf an unseren Shop auslöst.
  Alle Browser, die wir unterstützen, beherrschen das.

### Bestätigung

`AuthApiTests` (Registrieren, Anmelden, Abmelden, 401) und `AccessRulesTests` (401 für Gäste,
403 für Kunden, 201 für Admins). In der `.http`-Datei gibt es zu jeder Admin-Aktion einen
Aufruf ohne Anmeldung.

## Vor- und Nachteile der Optionen

### JWT

* Gut, weil das Backend nichts speichern muss.
* Schlecht, weil Abmelden nicht wirklich möglich ist, solange das Token gültig ist.
* Schlecht, weil das Token im Browser gespeichert werden muss (meist localStorage, angreifbar).

### HTTP Basic

* Gut, weil am einfachsten.
* Schlecht, weil das Passwort bei jedem Aufruf mitgeschickt wird und der Browser ein eigenes, nicht gestaltbares Anmeldefenster zeigt.

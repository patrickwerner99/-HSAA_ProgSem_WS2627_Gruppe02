---
status: "angenommen"
date: "2026-10-30"
decision-makers: "Gruppe 02"
---

# Hochgeladene Bilder im Docker-Volume, Auslieferung über die API

## Kontext und Problembeschreibung

Admins pflegen Produktbilder (D1), die API liefert Bilder als URL (D2), und die Daten müssen einen
Neustart überstehen (Q1). Wo speichern wir hochgeladene Dateien?

## Entscheidungstreiber

* Bilder überstehen Neustart und Neubau der Container
* Kein externer Dienst, kein API-Key (Q3)
* Datenbank bleibt klein
* Keine fremden Dateien vom Server lesbar

## Betrachtete Optionen

* Dateien in einem Docker-Volume (`/data/images`), ausgeliefert vom Backend
* Bilder als BLOB in der Datenbank
* Externer Speicher (z. B. S3, Cloudinary)

## Entscheidungsergebnis

Gewählte Option: „Docker-Volume“, weil es ohne zusätzliche Dienste funktioniert und die Bilder
genauso lange leben wie die Datenbank.

* Upload: `POST /api/products/{id}/image` (nur Admin). Das Backend prüft den Typ (PNG, JPG, WebP,
  GIF; kein SVG, weil SVG Skripte enthalten kann) und die Größe (5 MB), vergibt einen zufälligen
  Namen (`UUID.png`) und speichert die Datei im Volume `images`.
* In der Datenbank steht nur die URL, z. B. `/api/images/3f2c….png` (D2).
* `GET /api/images/{name}` liefert die Datei aus. Erlaubt sind nur Namen nach dem Muster
  UUID + Endung, damit niemand mit `../` andere Dateien lesen kann.
* Fehlt die Datei, zeigt das Frontend den Platzhalter (D3, `FallbackImageDirective`).

### Konsequenzen

* Gut, weil `docker compose down` die Bilder nicht löscht; erst `down -v`.
* Gut, weil Bilder lange gecacht werden dürfen (neuer Upload → neuer Name).
* Schlecht, weil Datenbank und Bilder getrennt gesichert werden müssten (für uns kein Thema).
* Schlecht, weil es so nur mit einem Backend-Container funktioniert.

### Bestätigung

`ImageApiTests`: Upload als Admin, Auslieferung mit richtigem Content-Type, 400 bei Text-Datei,
403 für Kunden, 404 für unbekannte Namen.

## Vor- und Nachteile der Optionen

### BLOB in der Datenbank

* Gut, weil alles an einem Ort liegt.
* Schlecht, weil die Datenbank groß wird und jede Produktabfrage aufpassen muss, die Bilder nicht mitzuladen.

### Externer Speicher

* Gut, weil skalierbar.
* Schlecht, weil Konto und API-Key nötig sind (Q3) und der Shop ohne Internet nicht startet.

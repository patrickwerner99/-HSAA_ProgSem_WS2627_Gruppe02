---
status: "erfüllt"
prioritaet: "Must"
sprint: "Sprint 2"
quelle: "Vorgabe Sprint 2, D1, D2, D3"
adr: "ADR-0007"
---

# Produktbilder

## Grobe Anforderung (Vorgabe)

> D1: Bilder, die der Admin pflegt.
> D2: API liefert Bilder als URL.
> D3: Fehlende Bilder machen die Seite nicht kaputt.

## UI/UX-Überlegung

* Im Produktformular Bereich „Produktbild“ mit Vorschau, „Bild hochladen…“ und „Bild entfernen“. Das Bild wird beim Speichern hochgeladen; bei neuen Produkten direkt nach dem Anlegen.
* Alternativ kann weiterhin eine Bild-Adresse eingetragen werden.
* **Entscheidung und Begründung:** Upload statt nur Adresse, weil Admins Bilder meist als Datei haben. Gespeichert wird im Docker-Volume, nicht in der Datenbank (ADR-0007).

## User Story

Als **Admin** möchte ich Produktbilder hochladen und austauschen, damit Kunden sehen, was sie kaufen.

## Akzeptanzkriterien

### Erwartetes Verhalten

* `POST /api/products/{id}/image` (multipart, Feld `file`, nur Admin) → 200 mit dem Produkt, `imageUrl` ist z. B. `/api/images/3f2c…png`.
* `GET /api/images/{name}` liefert das Bild mit passendem Content-Type (für alle).
* Ein neues Bild ersetzt das alte, die alte Datei wird gelöscht.
* `DELETE /api/products/{id}/image` entfernt das Bild; das Produkt zeigt den Platzhalter.
* Bilder überleben einen Neustart (`docker compose down` + `up`).

### Fehlerfälle

* Keine Bilddatei (z. B. .txt, .svg) → 400 „Bitte ein Bild im Format PNG, JPG, WebP oder GIF hochladen.“ Das Frontend prüft das schon vor dem Hochladen.
* Größer als 5 MB → 413 „Das Bild ist zu groß. Erlaubt sind höchstens 5 MB.“
* Bild fehlt oder lädt nicht (Datei gelöscht, Adresse kaputt) → Platzhalterbild, die Seite funktioniert weiter.
* Unbekannter Bildname → 404; Namen werden streng geprüft (UUID + Endung), damit niemand andere Dateien vom Server lesen kann.

## Tests

`ImageApiTests`, `product-list.spec.ts` („zeigt bei fehlendem Bild den Platzhalter“).

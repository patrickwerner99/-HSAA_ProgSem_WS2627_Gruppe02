---
status: "erfüllt"
prioritaet: "Must"
sprint: "Sprint 3"
quelle: "Review-Feedback Sprint 2, Erweiterung von C1"
adr: "ADR-0002"
---

# Nach Produktart filtern (Pulver, Riegel, Getränke …)

## Grobe Anforderung (Vorgabe)

> Review-Feedback Sprint 2: mehr Filteroptionen, z. B. „nur Riegel“.
> C1 (Sprint 2): Kunden können Produkte suchen, filtern und sortieren.

## UI/UX-Überlegung

* **Seiten:** Produktliste, Pflegeseite (Formular).
* **Entscheidung und Begründung:** Die Produktart ist ein **eigenes Merkmal** neben der Kategorie und keine weitere Kategorie. Kategorien beschreiben den Inhalt (Kreatin, Protein, …), die Art die Form (Pulver, Riegel, Getränk, Kapseln & Tabletten, Gel). So lässt sich beides kombinieren: „Protein“ + „Getränke“ zeigt nur die fertigen Shakes. Als Kategorie wäre „Proteinriegel“ entweder bei „Protein“ oder bei „Riegel“, aber nie bei beiden.
* Bedienung: Auswahl „Produktart“ neben „Sortieren nach“, Standard „Alle Produktarten“. In der URL steht ein lesbarer Wert (`?art=riegel`), damit Links weiter funktionieren (C2).
* Gefiltert wird im Backend, weil die Liste nur 12 Produkte auf einmal lädt (ADR-0002).

## User Story

Als **Kunde** möchte ich die Liste auf eine Produktart wie „Riegel“ oder „Getränke“ einschränken, damit ich schnell finde, was ich unterwegs oder im Wettkampf brauche.

## Akzeptanzkriterien

### Erwartetes Verhalten

* `GET /api/products?form=RIEGEL` liefert nur Produkte mit `form = RIEGEL` (Groß-/Kleinschreibung egal).
* Produktart, Kategorie, Suche, „Nur verfügbare“ und Sortierung lassen sich kombinieren, z. B. `?form=GETRAENK&categoryId=2&sort=price-asc`.
* Jedes Produkt liefert `form`; die Detailseite zeigt „Art: Riegel“.
* Im Frontend steht die Auswahl in der URL (`/?art=getraenke`) und wird beim Öffnen des Links wiederhergestellt. „Filter zurücksetzen“ setzt auch die Produktart zurück.
* Beim Anlegen und Ändern (POST/PUT) ist die Produktart Pflicht; PATCH kann sie einzeln ändern.

### Fehlerfälle

* Unbekannte Art über die API (`form=Kekse`) → **400** mit Feldfehler `form` und der Liste der erlaubten Werte.
* Unbekannte Art in der Frontend-URL (`?art=kekse`) → still „Alle Produktarten“, kein Fehler (wie bei der Sortierung).
* Produkt ohne Produktart anlegen → **400** mit Feldfehler `form`.
* Keine Treffer → „Keine Produkte gefunden“ mit Knopf „Filter zurücksetzen“ (wie 0001).

## Tests

`ProductApiTests` (filterByFormShowsOnlyBars, formCategoryAndSortCombine, unknownFormIsRejected, createWithoutFormIsAFieldError), `product.service.spec.ts` (Parameter `form`).

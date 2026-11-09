---
status: "erfüllt"
prioritaet: "Must"
sprint: "Sprint 2"
quelle: "Vorgabe Sprint 2, C2"
adr: "ADR-0002, ADR-0005"
---

# Jede Ansicht als Link speicherbar

## Grobe Anforderung (Vorgabe)

> C2: Jede Ansicht als Link speicherbar und sieht beim Öffnen gleich aus.

## Entscheidung

Alles, was bestimmt, was man sieht, steht in der URL. Kein Zustand nur im Speicher der Seite.

| Ansicht | Beispiel-Link |
| --- | --- |
| Produktliste mit Suche, Kategorie, Sortierung, Filter | `/?q=whey&kategorie=2&sort=price-desc&verfuegbar=ja` |
| Produkt | `/produkte/5` |
| Warenkorb | `/warenkorb` |
| Meine Bestellungen, Seite 2 | `/bestellungen?seite=2` |
| Eine Bestellung | `/bestellungen/12` |
| Pflege-Reiter | `/pflege/produkte`, `/pflege/kategorien`, `/pflege/bestellungen?status=RECEIVED` |

## User Story

Als **Nutzer** möchte ich eine Ansicht als Lesezeichen speichern oder verschicken, damit sie beim Öffnen genauso aussieht.

## Akzeptanzkriterien

* Jeder Link aus der Tabelle zeigt nach dem Öffnen in einem neuen Tab dieselbe Ansicht.
* Zurück/Vor im Browser funktioniert zwischen Ansichten.
* Braucht eine Ansicht eine Anmeldung, kommt man zuerst zur Anmeldung und danach auf genau diesen Link (`?zurueck=...`).
* Ungültige Werte in der URL (`sort=quatsch`, `seite=-3`, `status=FOO` in der Pflege) führen zur Standardansicht, nicht zu einer Fehlerseite.

## Grenzen

* Der **Inhalt** des Warenkorbs liegt im Browser (ADR-0005). Der Link `/warenkorb` öffnet auf einem anderen Gerät den dort gespeicherten Warenkorb.
* Beim endlosen Scrollen steht nicht in der URL, wie weit gescrollt wurde. Der Link zeigt die Liste von oben, mit gleichem Filter und gleicher Sortierung (ADR-0002).

## Tests

`product-list.spec.ts` („übernimmt Sortierung und Nur verfügbare aus der URL“), `auth.spec.ts` (Rücksprung-Link).

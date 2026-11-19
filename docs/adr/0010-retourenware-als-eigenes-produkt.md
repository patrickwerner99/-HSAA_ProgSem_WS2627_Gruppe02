---
status: "angenommen"
date: "2026-11-19"
decision-makers: "Gruppe 02"
---

# Originalverschlossene Retourenware als eigenes, reduziertes Produkt

## Kontext und Problembeschreibung

F2 verlangt, dass unbenutzt zurückgeschickte Ware (bei uns: originalverschlossen, weil es Lebensmittel sind) **als reduzierte Ware** wieder verkauft wird. Wie bilden wir das im
Datenmodell ab, ohne den normalen Preis und Bestand des Originalprodukts zu verfälschen?

## Entscheidungstreiber

* Der Kunde muss erkennen, dass es Rückläufer sind, und der reduzierte Preis gilt nur für diese Stücke.
* Der normale Bestand und Preis des Originals bleiben unverändert.
* Möglichst wenig neue Mechanik: Sale (G1), Warenkorb und Bestellung sollen ohne Sonderfall funktionieren.

## Betrachtete Optionen

* **Eigenes Produkt** „Retourenware“ mit eigenem Bestand und einem Sale ohne Enddatum
* Zweiter Bestand („B-Ware-Bestand“) und zweiter Preis am Originalprodukt
* Rückläufer einfach zum normalen Bestand zählen

## Entscheidungsergebnis

Gewählte Option: „Eigenes Produkt“, weil Warenkorb, Bestellung, Bestandssperre (ADR-0006) und Sale-Anzeige ohne
Änderung funktionieren.

* Beim Status `RECEIVED` sucht `ReturnService` ein Produkt mit `returnedGoods = true` und gleichem Namen und gleicher
  Größe. Gibt es keins, wird es angelegt (Kategorie, Art, Bild, Beschreibung wie das Original), mit Sale 30 % unter dem
  normalen Preis und ohne Enddatum („solange Vorrat reicht“).
* Originalverschlossene Stücke erhöhen dessen Bestand; geöffnete Stücke werden aus Hygienegründen nicht wieder verkauft.
* Das Produkt erscheint automatisch im Sale-Bereich und trägt in Liste und Detailseite den Hinweis „Retourenware“.

### Konsequenzen

* Gut, weil es keinen Sonderfall in Bestellung oder Warenkorb gibt.
* Gut, weil der Admin das Retourenprodukt wie jedes andere pflegen oder löschen kann.
* Schlecht, weil es in der Liste zwei Einträge mit gleichem Namen gibt; der Hinweis „Retourenware“ und der Preis
  unterscheiden sie.
* Schlecht, weil eine spätere Änderung am Original (z. B. Beschreibung) nicht automatisch übernommen wird.

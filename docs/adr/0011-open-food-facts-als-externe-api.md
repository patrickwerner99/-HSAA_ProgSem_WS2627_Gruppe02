---
status: "angenommen"
date: "2026-11-19"
decision-makers: "Gruppe 02"
---

# Open Food Facts als externe API für Nährwerte

## Kontext und Problembeschreibung

H1 verlangt Inhalte aus mindestens einer externen API, die zum Shop passen. Welche API nutzen wir?

## Entscheidungstreiber

* Der Inhalt muss für Kunden eines Sportnahrungs-Shops echten Nutzen haben.
* Kein API-Key und kein Konto, denn Geheimnisse dürfen nicht ins Repo (Q3) und jeder in der Gruppe muss den Shop mit
  `docker compose up` ohne Zusatzschritte starten können (Q1).
* Kostenlos, ausreichend stabil, verständliches JSON.

## Betrachtete Optionen

* **Open Food Facts** (offene Lebensmitteldatenbank, Nährwerte per Barcode)
* Wetter-API (z. B. Open-Meteo) für einen „Trainingswetter“-Hinweis
* Wechselkurs-API für Preise in anderen Währungen
* Kommerzielle Nährwert-APIs (z. B. Edamam, Nutritionix)

## Entscheidungsergebnis

Gewählte Option: „Open Food Facts“, weil Nährwerte (Eiweiß, Kalorien, Zucker) für Sportnahrung das wichtigste
Kaufkriterium sind und die API ohne Key auskommt.

* Abfrage: `GET https://world.openfoodfacts.org/api/v2/product/{ean}.json?fields=product_name,brands,nutriments,nutriscore_grade`
* Open Food Facts verlangt nur einen aussagekräftigen `User-Agent`; wir senden `HyroxShop/1.0 (Hochschule Aalen, Projektseminar)`.
* Die Daten stehen unter der Open Database License; wir nennen die Quelle mit Link auf der Produktseite.

### Konsequenzen

* Gut, weil der Inhalt direkt zum Sortiment passt und Kunden beim Vergleichen hilft.
* Gut, weil kein Geheimnis verwaltet werden muss.
* Schlecht, weil unsere Produkte ausgedacht sind: Wir hinterlegen den Barcode eines echten, vergleichbaren Produkts und
  zeigen dessen Namen und Marke offen an.
* Schlecht, weil Open Food Facts von Freiwilligen gepflegt wird: Werte können fehlen (dann „–“) und die API kann
  langsam sein. Wie wir damit umgehen, steht in ADR-0012.
* Die Wetter- und Währungs-APIs wären einfacher gewesen, passen aber kaum zu einem Nahrungsergänzungs-Shop.

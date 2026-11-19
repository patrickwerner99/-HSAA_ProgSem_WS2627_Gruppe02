---
status: "angenommen"
date: "2026-11-19"
decision-makers: "Gruppe 02"
---

# Externe API serverseitig anbinden, mit Timeout, Cache und Ersatzanzeige

## Kontext und Problembeschreibung

H2 verlangt, dass der Shop nutzbar bleibt, wenn die externe API (Open Food Facts, ADR-0011) langsam oder nicht
erreichbar ist. Wer ruft die API auf, und was passiert bei Problemen?

## Entscheidungstreiber

* Die Produktseite darf nie auf die fremde API warten oder wegen ihr ausfallen.
* Die fremde API soll nicht bei jedem Seitenaufruf belastet werden (Fairness, Rate-Limits).
* Fehler sollen im selben Format wie alle anderen ankommen (ADR-0003) und testbar sein.

## Betrachtete Optionen

* **Backend ruft die API auf** (eigener Endpunkt `/api/products/{id}/nutrition`), mit Timeout und Cache
* Browser ruft Open Food Facts direkt auf
* Nährwerte beim Start einmal laden und in der Datenbank speichern

## Entscheidungsergebnis

Gewählte Option: „Backend ruft die API auf“.

* **Eigener Endpunkt:** `GET /api/products/{id}` bleibt unabhängig; das Frontend lädt die Nährwerte erst danach.
* **Timeout:** Verbindung und Antwort je höchstens 2,5 s (`shop.openfoodfacts.timeout-ms`), über den
  `RestClient` mit `JdkClientHttpRequestFactory`. Danach gilt die API als nicht erreichbar.
* **Cache:** Antworten werden 24 Stunden im Speicher gehalten (`NutritionService`). Auch „unbekannt“ wird gemerkt.
* **Ersatz bei Ausfall:** Gibt es ältere Werte, liefern wir sie mit `stale: true` (Anzeige „Stand …“).
  Sonst **503** mit Problem Details; das Frontend zeigt „Nährwerte sind gerade nicht verfügbar“, der Rest der Seite
  funktioniert.
* Die Adresse der API ist einstellbar (`OPENFOODFACTS_URL`). Im Test zeigt sie auf einen kleinen lokalen Server, der
  Erfolg, 404, Fehler und langsame Antworten nachstellt.

### Konsequenzen

* Gut, weil Browser-Probleme (CORS, fremde Domain im Frontend) entfallen und Fehlerformat und Tests einheitlich sind.
* Gut, weil wiederholte Aufrufe schnell sind und Open Food Facts weniger belastet wird.
* Schlecht, weil der Cache bei einem Neustart leer ist; fällt die API genau dann aus, gibt es kurz keine Nährwerte.
  Für einen Shop dieser Größe reicht das; sonst müsste man die Werte in der Datenbank speichern.
* Schlecht, weil der Browser-Weg weniger Code gewesen wäre. Er hätte aber H2 schlechter erfüllt und wäre schwer testbar.

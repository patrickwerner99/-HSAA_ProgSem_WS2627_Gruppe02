---
status: "erfüllt"
prioritaet: "Must"
sprint: "Sprint 2"
quelle: "Vorgabe Sprint 2, A1"
adr: "keine"
---

# Meine Bestellungen

## Grobe Anforderung (Vorgabe)

> A1: Kunden haben ein eigenes Konto, mit dem sie bestellen und ihre Bestellungen wiederfinden.

## UI/UX-Überlegung

* `/bestellungen`: Liste der eigenen Bestellungen, neueste zuerst (Nr., Datum, Anzahl Artikel, Summe, Status). Filter nach Status und Seite stehen in der URL.
* `/bestellungen/{id}`: Positionen, Gesamtsumme, Status. Solange „Eingegangen“, gibt es „Bestellung stornieren“ (mit Sicherheitsabfrage).
* **Entscheidung und Begründung:** Stornieren nur bis zur Bearbeitung, danach ist die Ware evtl. schon gepackt. Welche Aktion möglich ist, sagt das Backend (`allowedNextStatuses`), das Frontend zeigt nur an.

## User Story

Als **Kunde** möchte ich meine bisherigen Bestellungen und ihren Status sehen, damit ich weiß, was ich bestellt habe und wo die Lieferung steht.

## Akzeptanzkriterien

* Gegeben ich bin angemeldet, wenn ich „Meine Bestellungen“ öffne, dann sehe ich nur **meine** Bestellungen.
* Die Bestellung eines anderen Kunden (`/bestellungen/7`) liefert **404** „Diese Bestellung gibt es nicht.“ (nicht 403, damit nicht verraten wird, dass es sie gibt).
* Stornieren im Status „Eingegangen“ → Status „Storniert“, der Bestand kommt zurück.
* Stornieren später → 409 „Diese Bestellung kann nicht mehr storniert werden …“.
* Andere Status als „Storniert“ darf ein Kunde nicht setzen → 403.
* Nicht angemeldet → Weiterleitung zur Anmeldung, danach zurück auf die Seite.

## Tests

`OrderApiTests.customersOnlySeeTheirOwnOrders`, `cancellingReturnsTheStock`, `customerCannotSetOtherStatusesOrCancelLate`.

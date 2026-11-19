---
status: "erfüllt"
prioritaet: "Must"
sprint: "Sprint 3"
quelle: "Sprint 3, E2 und E3"
adr: "ADR-0009"
---

# Mehrere Adressen speichern, eine pro Bestellung wählen

## Grobe Anforderung (Vorgabe)

> E2: Kunden können mehrere Adressen speichern und pro Bestellung eine davon auswählen.
> E3: Eine Bestellung behält die Adresse und die Preise, mit denen sie aufgegeben wurde.

## UI/UX-Überlegung

* **Seiten:** „Mein Konto“ → Adressen; Warenkorb → Lieferadresse.
* Jede Adresse hat eine Bezeichnung („Zuhause“, „Arbeit“). Eine Adresse ist **Standard** und im Warenkorb vorausgewählt.
* Im Warenkorb: Liste der gespeicherten Adressen zum Anklicken, darunter „Andere Adresse eingeben“. Eine neue Adresse
  kann mit „In meinem Konto speichern“ gleich für später gemerkt werden.
* **Entscheidung (ADR-0009):** Die Bestellung bekommt eine **Kopie** der Adresse. Ändert oder löscht der Kunde die
  gespeicherte Adresse später, bleibt die Bestellung unverändert (E3). Die Preise werden schon seit Sprint 2 in die
  Bestellpositionen kopiert (B3); Rabatt und Sale-Preis ebenfalls (0023, 0024).
* Höchstens 10 Adressen pro Konto, damit die Auswahl übersichtlich bleibt.

## User Story

Als **Kunde** möchte ich mehrere Lieferadressen speichern und beim Bestellen eine auswählen, damit ich z. B. mal nach
Hause und mal ins Büro liefern lassen kann, ohne jedes Mal alles neu einzutippen.

## Akzeptanzkriterien

### Erwartetes Verhalten

* `GET/POST /api/account/addresses`, `PUT/DELETE /api/account/addresses/{id}`.
* Die erste Adresse wird automatisch Standard. `"standard": true` an einer anderen macht diese zum Standard.
* Wird die Standardadresse gelöscht, wird die nächste Adresse Standard.
* `POST /api/orders` mit `addressId` nimmt die gespeicherte Adresse, mit `deliveryAddress` eine neu eingegebene.
* `GET /api/orders/{id}` zeigt dieselbe Adresse wie beim Bestellen, auch nach Ändern oder Löschen der gespeicherten.

### Fehlerfälle

* Straße ohne Hausnummer, PLZ nicht 5-stellig, Pflichtfeld leer → **400** mit Feldfehler.
* 11. Adresse → **409**.
* Adresse eines anderen Kunden lesen, ändern, löschen oder bei der Bestellung verwenden → **404**.
* Bestellung ohne `addressId` und ohne `deliveryAddress` → **400**, Feldfehler `deliveryAddress`.

## Tests

`AccountApiTests` (firstAddressBecomesStandard, anotherAddressCanBecomeStandard,
deletingTheStandardAddressMakesAnotherOneStandard, invalidAddressIsRejected, foreignAddressesAreInvisible,
atMostTenAddresses, orderWithSavedAddressKeepsACopy, foreignAddressCannotBeUsedForAnOrder), `cart.spec.ts`.

# Spickzettel Sprint 2: Konto, Warenkorb, Bestellungen, Bilder

Für das Review am 13.11.2026. Die Prüfer suchen aus, wer Code-Fragen beantwortet, also sollte jeder
die Abschnitte 1–6 erklären können. Zu jeder Frage steht, wo der Code liegt.

## 1. Wer darf was? (A1, A2)

```
Gast        lesen: Produkte, Kategorien, Bilder · Warenkorb im Browser · registrieren/anmelden
Kunde       + bestellen, eigene Bestellungen sehen, eigene Bestellung stornieren (nur "Eingegangen")
Admin       + Produkte, Kategorien, Bilder ändern · alle Bestellungen sehen, Status setzen
```

**Wo wird das durchgesetzt?** Im Backend, `security/SecurityConfig.java`:

```java
.requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**").permitAll()
.requestMatchers("/api/products", "/api/products/**").hasRole("ADMIN")   // alles andere an Produkten
.requestMatchers("/api/orders", "/api/orders/**").authenticated()
```

Die Reihenfolge zählt: Die erste passende Regel gewinnt. GET auf Produkte ist offen, POST/PUT/PATCH/DELETE
fallen auf die zweite Regel.

**Und das Frontend?** Blendet „Pflege“ aus und hat Guards (`shared/auth.ts`). Das ist nur Komfort.
Probe: `http/api.http` ruft die Admin-Endpunkte als Gast (401) und als Kunde (403) auf, ganz ohne Frontend.

**401 oder 403?** 401 = „Wer bist du?“ (nicht angemeldet). 403 = „Ich weiß, wer du bist, aber du darfst das nicht.“

**Warum bekommt ein Kunde für eine fremde Bestellung 404 statt 403?** Ein 403 würde verraten, dass es
Bestellung Nr. 7 gibt. `OrderService.loadVisible` tut so, als gäbe es sie nicht.

## 2. Anmelden (ADR-0004)

1. `POST /api/auth/login` mit E-Mail und Passwort → `AuthController.login`.
2. `AccountService.verify` lädt den Nutzer und vergleicht das Passwort mit dem **BCrypt-Hash**
   (`passwordEncoder.matches`). Falsches Passwort und unbekannte E-Mail geben dieselbe Meldung.
3. `startSession`: alte Sitzung weg, neue Sitzung an, darin der angemeldete Nutzer mit Rolle.
4. Der Browser bekommt das Cookie `JSESSIONID` (`HttpOnly`, `SameSite=Strict`) und schickt es ab jetzt
   bei jedem Aufruf mit. Spring Security liest daraus, wer anfragt.

**Was ist BCrypt?** Eine Einweg-Funktion mit Salt und absichtlich langsamer Berechnung. Aus dem Hash
kommt man nicht zurück zum Passwort; gleiche Passwörter haben verschiedene Hashes.

**Warum kein JWT?** Siehe ADR-0004: Cookie ist für Browser + gleiche Adresse einfacher, das Token liegt
nicht in JavaScript, und Abmelden wirkt sofort.

**Warum ist CSRF aus?** `SameSite=Strict`: Der Browser schickt unser Cookie nicht mit, wenn eine
fremde Seite einen Aufruf an unseren Shop auslöst. Damit läuft der klassische CSRF-Angriff ins Leere.

## 3. Warenkorb und Bestellen (B1–B3, ADR-0005, ADR-0006)

```
Detailseite ── "In den Warenkorb" ──▶ localStorage: [{ productId: 1, quantity: 2 }]
Warenkorb   ── lädt jedes Produkt frisch ──▶ GET /api/products/1   (Preis, Bestand aktuell)
"Bestellen" ── POST /api/orders { items: [{ productId: 1, quantity: 2 }] }
                    │
                    ▼  OrderService.place (eine Transaktion)
               1. Mengen pro Produkt zusammenzählen
               2. Produkte laden UND sperren (SELECT … FOR UPDATE)
               3. Bestand aller Positionen prüfen → fehlt was: 409 + Liste, nichts geändert
               4. Bestand abziehen, Preis aus der DB in die Position kopieren, Summe rechnen
               5. 201 + Location /api/orders/{id}
```

**B3: Was, wenn jemand im Browser den Preis auf 0,01 € ändert?** Bringt nichts. Im localStorage steht
gar kein Preis, und das Backend ignoriert jeden mitgeschickten Preis (`OrderRequest` kennt nur
`productId` und `quantity`). Test: `OrderApiTests.orderUsesServerPriceAndReducesStock`.

**B2: Zwei Kunden kaufen gleichzeitig das letzte Stück?** Die Sperre aus Schritt 2 lässt die zweite
Bestellung warten, bis die erste fertig ist. Danach sieht sie Bestand 0 → 409.

**Warum wird der Preis in die Bestellung kopiert?** Ändert der Admin später den Preis oder löscht das
Produkt, bleibt die Bestellung so, wie der Kunde sie abgeschickt hat.

**Warum Login erst beim Bestellen?** Stöbern ohne Hürde. Der Warenkorb liegt im Browser und überlebt
das Anmelden. Nach dem Login geht es über `?zurueck=/warenkorb` zurück.

## 4. Bestellstatus (B4)

```
RECEIVED ──▶ PROCESSING ──▶ SHIPPED ──▶ DELIVERED
   │              │
   └──▶ CANCELLED ◀┘     (Stornieren gibt den Bestand zurück)
```

Die erlaubten Übergänge stehen an genau einer Stelle: `OrderStatus.next()`. Die API liefert pro
Bestellung `allowedNextStatuses` (Admin: laut Tabelle, Kunde: nur Storno solange RECEIVED), das
Frontend baut daraus Menü und Knöpfe. Unerlaubter Übergang per API → 409.

## 5. Suchen, Filtern, Sortieren, Links (C1, C2)

- Sortiert wird in der Datenbank (`ProductService.toSort`), weil wir nur 12 Produkte pro Ladung holen
  (ADR-0002). Bei gleichem Preis entscheidet der Name, sonst würden beim Nachladen Produkte doppelt
  kommen oder fehlen.
- Alles, was die Ansicht bestimmt, steht in der URL: `/?q=whey&kategorie=2&sort=price-asc&verfuegbar=ja`,
  `/bestellungen?status=SHIPPED&seite=2`, `/pflege/bestellungen`. Die Seite liest beim Öffnen nur die URL
  (`route.queryParamMap`), Klicks ändern nur die URL. So sieht ein geteilter Link immer gleich aus.
- Grenze: Der Warenkorb-**Inhalt** hängt am Browser (ADR-0005).

## 6. Bilder (D1–D3, ADR-0007)

- Upload: `POST /api/products/{id}/image` (multipart). `ImageStorage.store` prüft Typ und Größe, vergibt
  einen Zufallsnamen (`UUID.png`) und speichert im Volume `images`. In der DB steht nur die URL `/api/images/…` (D2).
- Warum kein SVG? SVG kann JavaScript enthalten.
- Warum Zufallsnamen? Niemand kann fremde Dateien überschreiben oder Namen erraten; beim Abrufen
  werden nur Namen nach dem Muster UUID + Endung angenommen, `../etc/passwd` geht nicht.
- D3: `FallbackImageDirective` ersetzt jedes Bild, das nicht lädt, durch den Platzhalter.

## 7. Betrieb (Q1–Q3, ADR-0008)

- `docker compose up -d --build` startet alles. Daten liegen in den Volumes `db-data` und `images`.
- Admin-Passwort: zufällig beim ersten Start, steht im Log (README). Kein Passwort im Repo.
- `docker compose down -v` setzt alles zurück, inklusive neuem Admin-Passwort.

## Typische Prüferfragen – Kurzantworten

| Frage | Antwort |
| --- | --- |
| Wo wird geprüft, ob jemand Admin ist? | `SecurityConfig` (URL + Methode), bei Bestellungen zusätzlich `OrderService` |
| Was passiert bei 409 im Warenkorb? | Frontend lädt Produkte neu, markiert die Position („Nur noch N verfügbar“), Bestellen gesperrt bis angepasst |
| Wie testet ihr das? | `OrderApiTests`, `AccessRulesTests`, `AuthApiTests`, `ImageApiTests` mit MockMvc gegen H2; Frontend mit Vitest |
| Warum H2 in Tests? | Schnell, ohne Docker. Risiko: SQL-Unterschiede; deshalb nur JPA-Abfragen, keine MariaDB-Spezialitäten |
| Was liegt im localStorage? | Nur Produkt-ID und Menge, kein Preis, keine Anmeldedaten |
| Wo steht das Passwort? | Nirgends im Klartext: BCrypt-Hash in `app_user.password_hash` |
| Warum ist die Bestellung „alles oder nichts“? | `@Transactional`: Eine Exception rollt alle Änderungen zurück, Bestand wird erst nach der Prüfung abgezogen |

---
status: "erfüllt"
prioritaet: "Must"
sprint: "Sprint 2"
quelle: "Vorgabe Sprint 2, A1"
adr: "ADR-0004"
---

# Kundenkonto: registrieren, anmelden, abmelden

## Grobe Anforderung (Vorgabe)

> A1: Kunden haben ein eigenes Konto, mit dem sie bestellen und ihre Bestellungen wiederfinden.

## UI/UX-Überlegung

* **Seiten:** `/anmelden` und `/registrieren`, je ein kurzes Formular. In der Kopfzeile „Anmelden“ bzw. „Abmelden (Name)“.
* **Ablauf:** Einkaufen geht ohne Konto. Erst beim Klick auf „Anmelden und bestellen“ im Warenkorb geht es zur Anmeldung, danach automatisch zurück zum Warenkorb (`?zurueck=/warenkorb`). Nach dem Registrieren ist man sofort angemeldet.
* **Entscheidung und Begründung:** Login erst beim Bestellen, weil eine Pflicht-Anmeldung vor dem Stöbern Kunden abschreckt. Der Warenkorb bleibt dabei erhalten (liegt im Browser, ADR-0005).
* **Mögliche Fehlerstellen:** E-Mail schon vergeben, falsches Passwort, zu kurzes Passwort, Sitzung abgelaufen.

## User Story

Als **Kunde** möchte ich ein eigenes Konto anlegen und mich anmelden, damit ich bestellen und meine Bestellungen später wiederfinden kann.

## Akzeptanzkriterien

### Erwartetes Verhalten

* Gegeben ich bin nicht angemeldet, wenn ich Name, E-Mail und ein Passwort mit mindestens 8 Zeichen eingebe und „Konto anlegen“ klicke, dann ist mein Konto angelegt und ich bin angemeldet.
* Gegeben ich habe ein Konto, wenn ich mich mit E-Mail und Passwort anmelde, dann sehe ich oben „Abmelden (Name)“ und „Meine Bestellungen“.
* Groß-/Kleinschreibung der E-Mail spielt keine Rolle („Lena@Example.de“ = „lena@example.de“).
* Gegeben ich bin angemeldet, wenn ich „Abmelden“ klicke, dann ist die Sitzung im Backend beendet.
* Das Passwort wird nur als BCrypt-Hash gespeichert und nie von der API zurückgegeben.

### Fehlerfälle

* E-Mail schon vergeben → 409 „Zu dieser E-Mail-Adresse gibt es schon ein Konto. Bitte melde dich an.“
* Falsches Passwort **oder** unbekannte E-Mail → 401 mit derselben Meldung „E-Mail oder Passwort ist falsch.“ (verrät nicht, welche E-Mails es gibt).
* Ungültige Eingaben (leerer Name, keine E-Mail, Passwort < 8 Zeichen) → 400, Meldungen direkt am Feld.

## Tests

`AuthApiTests` (Backend), `auth.spec.ts` (Frontend: Rücksprung nur auf eigene Seiten).

import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { parsePrice, parseStock } from '../../shared/format';

// Prüfungen im Formular für schnelle Rückmeldung. Verbindlich sind die gleichen Regeln
// im Backend (ProductRequest.java, ADR-0003).

/** Nur Leerzeichen zählt als leer. */
export const notBlank: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = control.value;
  return typeof value === 'string' && value.length > 0 && value.trim().length === 0
    ? { blank: true }
    : null;
};

/** Preis wie "34,90": größer 0, höchstens 9.999,99, max. zwei Nachkommastellen. */
export const priceValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  if (control.value === null || control.value === '') {
    return null; // "required" meldet das
  }
  if (String(control.value).trim().startsWith('-')) {
    return { pricePositive: true };
  }
  const price = parsePrice(control.value);
  if (price === null) {
    return { priceFormat: true };
  }
  if (price <= 0) {
    return { pricePositive: true };
  }
  if (price > 9999.99) {
    return { priceMax: true };
  }
  return null;
};

/** Bestand: ganze Zahl ab 0. */
export const stockValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  if (control.value === null || control.value === '') {
    return null;
  }
  return parseStock(control.value) === null ? { stockWhole: true } : null;
};

/** Meldung je Fehlerart. "server" ist eine Meldung, die das Backend zu diesem Feld geschickt hat. */
const MESSAGES: Record<string, Record<string, string>> = {
  name: {
    required: 'Bitte einen Namen eingeben.',
    blank: 'Bitte einen Namen eingeben.',
    minlength: 'Der Name muss 2 bis 100 Zeichen lang sein.',
    maxlength: 'Der Name muss 2 bis 100 Zeichen lang sein.',
  },
  categoryName: {
    required: 'Bitte einen Namen eingeben.',
    blank: 'Bitte einen Namen eingeben.',
    minlength: 'Der Name muss 2 bis 50 Zeichen lang sein.',
    maxlength: 'Der Name muss 2 bis 50 Zeichen lang sein.',
  },
  categoryId: { required: 'Bitte eine Kategorie auswählen.' },
  price: {
    required: 'Bitte einen Preis eingeben.',
    priceFormat: 'Bitte einen Preis wie 34,90 eingeben.',
    pricePositive: 'Der Preis muss größer als 0 sein.',
    priceMax: 'Der Preis darf höchstens 9.999,99 € betragen.',
  },
  stock: {
    required: 'Bitte einen Bestand eingeben.',
    stockWhole: 'Der Bestand muss eine ganze Zahl ab 0 sein.',
  },
  description: { maxlength: 'Die Beschreibung darf höchstens 1.000 Zeichen lang sein.' },
  imageUrl: {
    pattern: 'Bitte eine gültige Bild-Adresse (http/https) eingeben oder das Feld leer lassen.',
    maxlength: 'Die Bild-Adresse ist zu lang.',
  },
};

/** Die erste passende Meldung für ein Feld, oder null wenn alles stimmt. */
export function messageFor(field: string, control: AbstractControl): string | null {
  const errors = control.errors;
  if (!errors) {
    return null;
  }
  if (typeof errors['server'] === 'string') {
    return errors['server'];
  }
  for (const key of Object.keys(errors)) {
    const message = MESSAGES[field]?.[key];
    if (message) {
      return message;
    }
  }
  return 'Bitte prüfe diese Eingabe.';
}

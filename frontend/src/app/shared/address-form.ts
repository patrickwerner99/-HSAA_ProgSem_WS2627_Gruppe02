import { FormControl, FormGroup, Validators } from '@angular/forms';
import { DeliveryAddress, FieldMessage } from '../api/models';
import { notBlank } from '../pages/admin/validators';

// Ein Adressformular für Warenkorb (0017) und Konto (0021). Gleiche Regeln wie im Backend
// (AddressRules.java); verbindlich prüft das Backend (ADR-0003).

export type AddressField = 'name' | 'street' | 'postalCode' | 'city';
export type AddressForm = FormGroup<{
  name: FormControl<string>;
  street: FormControl<string>;
  postalCode: FormControl<string>;
  city: FormControl<string>;
}>;

/** Buchstaben, dann irgendwo eine Ziffer: "Beethovenstraße 1", "Am Markt 3a". */
const STREET_PATTERN = /^(?=.*\p{L})(?=.*\d).+$/su;

export function createAddressForm(): AddressForm {
  return new FormGroup({
    name: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        notBlank,
        Validators.minLength(2),
        Validators.maxLength(100),
      ],
    }),
    street: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        notBlank,
        Validators.maxLength(100),
        Validators.pattern(STREET_PATTERN),
      ],
    }),
    postalCode: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/^\d{5}$/)],
    }),
    city: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        notBlank,
        Validators.minLength(2),
        Validators.maxLength(60),
      ],
    }),
  });
}

const MESSAGES: Record<AddressField, (errors: Record<string, unknown>) => string> = {
  name: (e) =>
    e['required'] || e['blank']
      ? 'Bitte gib den Namen des Empfängers an.'
      : 'Der Name muss 2 bis 100 Zeichen lang sein.',
  street: (e) =>
    e['required'] || e['blank']
      ? 'Bitte gib Straße und Hausnummer an.'
      : e['maxlength']
        ? 'Höchstens 100 Zeichen.'
        : 'Bitte mit Hausnummer, z. B. Beethovenstraße 1.',
  postalCode: (e) =>
    e['required'] ? 'Bitte gib die Postleitzahl an.' : 'Die Postleitzahl hat genau 5 Ziffern.',
  city: (e) =>
    e['required'] || e['blank']
      ? 'Bitte gib den Ort an.'
      : 'Der Ort muss 2 bis 60 Zeichen lang sein.',
};

export function addressMessage(form: AddressForm, field: AddressField): string {
  const errors = form.controls[field].errors ?? {};
  if (typeof errors['server'] === 'string') {
    return errors['server'];
  }
  return MESSAGES[field](errors);
}

/** Werte ohne überflüssige Leerzeichen. */
export function addressValue(form: AddressForm): DeliveryAddress {
  const { name, street, postalCode, city } = form.getRawValue();
  return {
    name: name.trim(),
    street: street.trim(),
    postalCode: postalCode.trim(),
    city: city.trim(),
  };
}

/**
 * Feldfehler vom Backend an die Felder hängen. Beim Bestellen heißen sie "deliveryAddress.city",
 * im Konto nur "city". Gibt zurück, ob ein Adressfeld dabei war.
 */
export function applyAddressErrors(
  form: AddressForm,
  errors: FieldMessage[],
  prefix = '',
): boolean {
  let found = false;
  for (const { field, message } of errors) {
    if (!field.startsWith(prefix)) {
      continue;
    }
    const name = field.slice(prefix.length).replace(/^\./, '') || 'name';
    const control = form.get(name);
    if (control) {
      control.setErrors({ server: message });
      control.markAsTouched();
      found = true;
    }
  }
  return found;
}

/** Eine Zeile zum Anzeigen: "Lena Muster, Beethovenstraße 1, 73430 Aalen". */
export function addressLine(address: DeliveryAddress): string {
  return `${address.name}, ${address.street}, ${address.postalCode} ${address.city}`;
}

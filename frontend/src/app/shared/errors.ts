import { HttpErrorResponse } from '@angular/common/http';
import { FieldMessage } from '../api/models';

// Übersetzt Fehler der API in Meldungen für Menschen (docs/anforderungen/0005).
// Nie technische Details anzeigen: kein Statuscode, keine Exception, kein "undefined".

export const OFFLINE_MESSAGE =
  'Der Shop ist gerade nicht erreichbar. Bitte versuche es gleich noch einmal.';
export const GENERIC_MESSAGE = 'Da ist etwas schiefgelaufen. Bitte versuche es erneut.';

/** Status, bei denen das Backend gar nicht antwortet (0 = Netzwerk, 502–504 = nginx findet kein Backend). */
const UNREACHABLE = [0, 502, 503, 504];

export function isUnreachable(error: unknown): boolean {
  return error instanceof HttpErrorResponse && UNREACHABLE.includes(error.status);
}

/** Die Meldung, die der Nutzer sieht. Bei 4xx kommt sie vom Backend (Feld "detail"). */
export function userMessage(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) {
    return GENERIC_MESSAGE;
  }
  if (UNREACHABLE.includes(error.status)) {
    return OFFLINE_MESSAGE;
  }
  const detail = error.error?.detail;
  if (error.status >= 400 && error.status < 500 && typeof detail === 'string' && detail) {
    return detail;
  }
  return GENERIC_MESSAGE;
}

/** Feldfehler aus einer 400-Antwort, z. B. [{ field: 'price', message: '...' }]. */
export function fieldErrors(error: unknown): FieldMessage[] {
  if (
    error instanceof HttpErrorResponse &&
    error.status === 400 &&
    Array.isArray(error.error?.errors)
  ) {
    return error.error.errors as FieldMessage[];
  }
  return [];
}

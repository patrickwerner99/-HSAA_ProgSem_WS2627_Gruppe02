import { HttpErrorResponse } from '@angular/common/http';
import { GENERIC_MESSAGE, OFFLINE_MESSAGE, fieldErrors, userMessage } from './errors';

const httpError = (status: number, error: unknown = null) =>
  new HttpErrorResponse({ status, error });

describe('userMessage', () => {
  it('meldet "nicht erreichbar", wenn das Backend nicht antwortet', () => {
    expect(userMessage(httpError(0))).toBe(OFFLINE_MESSAGE);
    expect(userMessage(httpError(502))).toBe(OFFLINE_MESSAGE);
  });

  it('übernimmt die Meldung des Backends bei 4xx', () => {
    const error = httpError(409, { detail: 'Diese Kategorie gibt es schon.' });
    expect(userMessage(error)).toBe('Diese Kategorie gibt es schon.');
  });

  it('zeigt bei Serverfehlern keine technischen Details', () => {
    const error = httpError(500, { detail: 'NullPointerException at ...' });
    expect(userMessage(error)).toBe(GENERIC_MESSAGE);
  });
});

describe('fieldErrors', () => {
  it('liest Feldfehler aus einer 400-Antwort', () => {
    const errors = [{ field: 'price', message: 'Der Preis muss größer als 0 sein.' }];
    expect(fieldErrors(httpError(400, { errors }))).toEqual(errors);
  });

  it('liefert eine leere Liste bei anderen Fehlern', () => {
    expect(fieldErrors(httpError(500))).toEqual([]);
  });
});

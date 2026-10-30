import { registerLocaleData } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import localeDe from '@angular/common/locales/de';
import {
  ApplicationConfig,
  DEFAULT_CURRENCY_CODE,
  LOCALE_ID,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter, withComponentInputBinding, withInMemoryScrolling } from '@angular/router';
import { routes } from './app.routes';

// Deutsche Formate: Preis "34,90 €", Datum "30.10.2026".
registerLocaleData(localeDe);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(),
    provideRouter(
      routes,
      // Route-Parameter wie :id kommen direkt als input() in die Komponente.
      withComponentInputBinding(),
      // Neue Seite → nach oben scrollen, Zurück → alte Position (wichtig fürs endlose Scrollen).
      withInMemoryScrolling({ scrollPositionRestoration: 'enabled' }),
    ),
    { provide: LOCALE_ID, useValue: 'de-DE' },
    { provide: DEFAULT_CURRENCY_CODE, useValue: 'EUR' },
  ],
};

import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { AuthService } from '../api/auth.service';

/**
 * Nur eigene Seiten als Rücksprung zulassen ("/warenkorb"), keine fremden Adressen
 * ("https://…" oder "//…"), sonst ließe sich die Anmeldung für Weiterleitungen missbrauchen.
 */
export function safeReturnUrl(value: string | null | undefined): string {
  if (!value || !value.startsWith('/') || value.startsWith('//') || value.startsWith('/\\')) {
    return '/';
  }
  return value;
}

/** Seite nur für angemeldete Nutzer; sonst zur Anmeldung und danach zurück hierher. */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth
    .load()
    .pipe(
      map((user) =>
        user ? true : router.createUrlTree(['/anmelden'], { queryParams: { zurueck: state.url } }),
      ),
    );
};

/**
 * Pflegeseite nur für Admins. Das ist nur Komfort: Die eigentliche Sperre sitzt im Backend (A2),
 * dort wird jeder Aufruf geprüft, auch ohne diese Seite.
 */
export const adminGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.load().pipe(
    map((user) => {
      if (!user) {
        return router.createUrlTree(['/anmelden'], { queryParams: { zurueck: state.url } });
      }
      return user.role === 'ADMIN' ? true : router.createUrlTree(['/kein-zugriff']);
    }),
  );
};

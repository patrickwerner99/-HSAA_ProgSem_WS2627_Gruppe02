import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { Observable, firstValueFrom } from 'rxjs';
import { adminGuard, authGuard, safeReturnUrl } from './auth';

describe('safeReturnUrl', () => {
  it('lässt eigene Seiten zu', () => {
    expect(safeReturnUrl('/warenkorb')).toBe('/warenkorb');
    expect(safeReturnUrl('/pflege/bestellungen?status=RECEIVED')).toBe(
      '/pflege/bestellungen?status=RECEIVED',
    );
  });

  it('leitet nie auf fremde Seiten weiter', () => {
    expect(safeReturnUrl('https://boese.example')).toBe('/');
    expect(safeReturnUrl('//boese.example')).toBe('/');
    expect(safeReturnUrl('/\\boese.example')).toBe('/');
    expect(safeReturnUrl(undefined)).toBe('/');
  });
});

describe('Guards', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });

  const run = (guard: typeof authGuard, url: string) =>
    TestBed.runInInjectionContext(() =>
      firstValueFrom(
        guard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot) as Observable<
          boolean | UrlTree
        >,
      ),
    );

  it('schickt Gäste zur Anmeldung und merkt sich die Seite', async () => {
    const result = run(authGuard, '/bestellungen');
    http.expectOne('/api/auth/me').flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(String(await result)).toBe('/anmelden?zurueck=%2Fbestellungen');
  });

  it('lässt Kunden nicht in die Pflege', async () => {
    const result = run(adminGuard, '/pflege/produkte');
    http
      .expectOne('/api/auth/me')
      .flush({ id: 2, email: 'kunde@example.de', name: 'Kunde', role: 'CUSTOMER' });
    expect(String(await result)).toBe('/kein-zugriff');
  });

  it('lässt Admins in die Pflege', async () => {
    const result = run(adminGuard, '/pflege/produkte');
    http
      .expectOne('/api/auth/me')
      .flush({ id: 1, email: 'admin@hyrox-shop.local', name: 'Admin', role: 'ADMIN' });
    expect(await result).toBe(true);
  });
});

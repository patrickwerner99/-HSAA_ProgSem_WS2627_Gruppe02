import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, of, tap } from 'rxjs';
import { User } from './models';

export interface LoginInput {
  email: string;
  password: string;
}

export interface RegisterInput extends LoginInput {
  name: string;
}

/**
 * Anmeldung über eine Sitzung (Cookie, ADR-0004). Das Frontend merkt sich nur, wer angemeldet ist,
 * um Menüpunkte ein- und auszublenden. Ob etwas erlaubt ist, entscheidet immer das Backend (A2).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  /** undefined = noch nicht geprüft, null = nicht angemeldet. */
  private readonly state = signal<User | null | undefined>(undefined);

  readonly user = computed(() => this.state() ?? null);
  readonly loggedIn = computed(() => this.user() !== null);
  readonly isAdmin = computed(() => this.user()?.role === 'ADMIN');

  /** Beim Start der App: Gibt es noch eine Sitzung? Fehler (z. B. Backend aus) → nicht angemeldet. */
  load(): Observable<User | null> {
    const known = this.state();
    if (known !== undefined) {
      return of(known);
    }
    return this.http.get<User>('/api/auth/me').pipe(
      catchError(() => of(null)),
      tap((user) => this.state.set(user)),
    );
  }

  login(input: LoginInput): Observable<User> {
    return this.http.post<User>('/api/auth/login', input).pipe(tap((user) => this.state.set(user)));
  }

  register(input: RegisterInput): Observable<User> {
    return this.http
      .post<User>('/api/auth/register', input)
      .pipe(tap((user) => this.state.set(user)));
  }

  logout(): Observable<void> {
    return this.http.post<void>('/api/auth/logout', null).pipe(
      // Auch wenn das Backend nicht antwortet: im Browser gilt man danach als abgemeldet.
      catchError(() => of(undefined)),
      tap(() => this.state.set(null)),
    );
  }
}

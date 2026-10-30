import { Component, inject, input, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../api/auth.service';
import { safeReturnUrl } from '../../shared/auth';
import { userMessage } from '../../shared/errors';

/** /anmelden?zurueck=/warenkorb – nach der Anmeldung geht es dorthin zurück (docs/anforderungen/0006). */
@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, MatFormFieldModule, MatInputModule],
  template: `
    <section class="account-box">
      <h1>Anmelden</h1>
      @if (zurueck() === '/warenkorb') {
        <p class="info">Zum Bestellen brauchst du ein Konto. Dein Warenkorb bleibt erhalten.</p>
      }
      @if (error(); as message) {
        <p class="form-error" role="alert">{{ message }}</p>
      }
      <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>E-Mail</mat-label>
          <input matInput type="email" formControlName="email" autocomplete="username" required />
          <mat-error>Bitte gib deine E-Mail-Adresse ein.</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Passwort</mat-label>
          <input
            matInput
            type="password"
            formControlName="password"
            autocomplete="current-password"
            required
          />
          <mat-error>Bitte gib dein Passwort ein.</mat-error>
        </mat-form-field>
        <button mat-flat-button type="submit" [disabled]="busy()">
          {{ busy() ? 'Anmelden…' : 'Anmelden' }}
        </button>
      </form>
      <p class="switch">
        Noch kein Konto?
        <a routerLink="/registrieren" [queryParams]="{ zurueck: zurueck() }">Jetzt registrieren</a>
      </p>
    </section>
  `,
  styleUrl: './account.css',
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** Query-Parameter ?zurueck=… (withComponentInputBinding). */
  readonly zurueck = input<string | undefined>();

  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly form = new FormGroup({
    email: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  protected submit(): void {
    if (this.form.invalid || this.busy()) {
      this.form.markAllAsTouched();
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    const { email, password } = this.form.getRawValue();
    this.auth.login({ email: email.trim(), password }).subscribe({
      next: () => this.router.navigateByUrl(safeReturnUrl(this.zurueck())),
      error: (error) => {
        this.busy.set(false);
        this.error.set(userMessage(error));
      },
    });
  }
}

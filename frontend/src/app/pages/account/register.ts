import { Component, inject, input, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../api/auth.service';
import { safeReturnUrl } from '../../shared/auth';
import { fieldErrors, userMessage } from '../../shared/errors';
import { notBlank } from '../admin/validators';

/** /registrieren – Konto anlegen; danach ist man gleich angemeldet (docs/anforderungen/0006). */
@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, MatFormFieldModule, MatInputModule],
  template: `
    <section class="account-box">
      <h1>Konto anlegen</h1>
      @if (error(); as message) {
        <p class="form-error" role="alert">{{ message }}</p>
      }
      <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Name</mat-label>
          <input matInput formControlName="name" autocomplete="name" required />
          <mat-error>{{ message('name') }}</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>E-Mail</mat-label>
          <input matInput type="email" formControlName="email" autocomplete="email" required />
          <mat-error>{{ message('email') }}</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Passwort</mat-label>
          <input
            matInput
            type="password"
            formControlName="password"
            autocomplete="new-password"
            required
          />
          <mat-hint>Mindestens 8 Zeichen.</mat-hint>
          <mat-error>{{ message('password') }}</mat-error>
        </mat-form-field>
        <button mat-flat-button type="submit" [disabled]="busy()">
          {{ busy() ? 'Wird angelegt…' : 'Konto anlegen' }}
        </button>
      </form>
      <p class="switch">
        Schon ein Konto?
        <a routerLink="/anmelden" [queryParams]="{ zurueck: zurueck() }">Anmelden</a>
      </p>
    </section>
  `,
  styleUrl: './account.css',
})
export class RegisterPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly zurueck = input<string | undefined>();

  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly form = new FormGroup({
    name: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        notBlank,
        Validators.minLength(2),
        Validators.maxLength(100),
      ],
    }),
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email, Validators.maxLength(254)],
    }),
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(8), Validators.maxLength(72)],
    }),
  });

  protected message(field: 'name' | 'email' | 'password'): string {
    const errors = this.form.controls[field].errors ?? {};
    if (errors['server']) {
      return errors['server'];
    }
    if (errors['required'] || errors['blank']) {
      return field === 'name'
        ? 'Bitte gib deinen Namen ein.'
        : field === 'email'
          ? 'Bitte gib deine E-Mail-Adresse ein.'
          : 'Bitte wähle ein Passwort.';
    }
    if (field === 'email') {
      return 'Bitte gib eine gültige E-Mail-Adresse ein.';
    }
    if (field === 'password') {
      return errors['maxlength'] ? 'Höchstens 72 Zeichen.' : 'Mindestens 8 Zeichen.';
    }
    return errors['maxlength'] ? 'Höchstens 100 Zeichen.' : 'Mindestens 2 Zeichen.';
  }

  protected submit(): void {
    if (this.form.invalid || this.busy()) {
      this.form.markAllAsTouched();
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    const { name, email, password } = this.form.getRawValue();
    this.auth.register({ name: name.trim(), email: email.trim(), password }).subscribe({
      next: () => this.router.navigateByUrl(safeReturnUrl(this.zurueck())),
      error: (error) => {
        this.busy.set(false);
        const errors = fieldErrors(error);
        for (const { field, message } of errors) {
          const control = this.form.get(field);
          control?.setErrors({ server: message });
          control?.markAsTouched();
        }
        this.error.set(
          errors.length > 0 ? 'Bitte prüfe die markierten Felder.' : userMessage(error),
        );
      },
    });
  }
}

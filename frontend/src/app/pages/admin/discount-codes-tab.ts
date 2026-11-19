import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { DiscountService } from '../../api/discount.service';
import { DiscountCode } from '../../api/models';
import { fieldErrors, userMessage } from '../../shared/errors';
import { NotifyService } from '../../shared/notify.service';

/** Heute als yyyy-MM-dd (für <input type="date">). */
function isoDate(date = new Date()): string {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 10);
}

/**
 * Reiter "Rabattcodes" der Pflegeseite (Sprint 3, G2, docs/anforderungen/0024): anlegen, ab- und
 * einschalten, löschen (nur nie benutzte). Die Regeln prüft verbindlich das Backend.
 */
@Component({
  selector: 'app-discount-codes-tab',
  imports: [DatePipe, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  template: `
    <form class="new-code" [formGroup]="form" (ngSubmit)="create()" novalidate>
      <h2>Neuer Rabattcode</h2>
      @if (formError(); as message) {
        <p class="form-error" role="alert">{{ message }}</p>
      }
      <div class="fields">
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Code</mat-label>
          <input matInput formControlName="code" placeholder="z. B. HYROX10" required />
          <mat-error>{{
            message('code', '3 bis 20 Zeichen: Buchstaben, Ziffern und -.')
          }}</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Rabatt</mat-label>
          <input matInput type="number" formControlName="percent" min="1" max="50" required />
          <span matTextSuffix>&nbsp;%</span>
          <mat-error>{{ message('percent', 'Zwischen 1 und 50 %.') }}</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>gültig ab</mat-label>
          <input matInput type="date" formControlName="validFrom" required />
          <mat-error>{{ message('validFrom', 'Bitte ein Datum wählen.') }}</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>gültig bis</mat-label>
          <input matInput type="date" formControlName="validUntil" required />
          <mat-error>{{ message('validUntil', 'Bitte ein Datum wählen.') }}</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>höchstens … mal (optional)</mat-label>
          <input matInput type="number" formControlName="maxUses" min="1" />
          <mat-error>{{ message('maxUses', 'Mindestens 1, oder leer lassen.') }}</mat-error>
        </mat-form-field>
      </div>
      <button mat-flat-button type="submit" [disabled]="saving()">
        {{ saving() ? 'Speichert…' : 'Code anlegen' }}
      </button>
    </form>

    @if (loadError(); as message) {
      <p class="form-error" role="alert">
        {{ message }} <button mat-button type="button" (click)="load()">Erneut versuchen</button>
      </p>
    }

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th scope="col">Code</th>
            <th scope="col" class="num">Rabatt</th>
            <th scope="col">Zeitraum</th>
            <th scope="col" class="num">Eingelöst</th>
            <th scope="col">Status</th>
            <th scope="col"><span class="visually-hidden">Aktionen</span></th>
          </tr>
        </thead>
        <tbody>
          @for (code of codes(); track code.id) {
            <tr>
              <td class="name">{{ code.code }}</td>
              <td class="num">{{ code.percent }} %</td>
              <td>
                {{ code.validFrom | date: 'dd.MM.yyyy' }} –
                {{ code.validUntil | date: 'dd.MM.yyyy' }}
              </td>
              <td class="num">
                {{ code.timesUsed }}{{ code.maxUses ? ' / ' + code.maxUses : '' }}
              </td>
              <td>
                @if (!code.active) {
                  abgeschaltet
                } @else if (code.usableToday) {
                  <strong>gültig</strong>
                } @else {
                  nicht gültig (Zeitraum oder Anzahl)
                }
              </td>
              <td class="actions">
                <button mat-stroked-button type="button" (click)="toggle(code)">
                  {{ code.active ? 'Abschalten' : 'Einschalten' }}
                </button>
                @if (code.timesUsed === 0) {
                  <button mat-button type="button" class="delete" (click)="remove(code)">
                    Löschen
                  </button>
                }
              </td>
            </tr>
          } @empty {
            <tr>
              <td colspan="6">Noch keine Rabattcodes.</td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
  styleUrl: './admin-table.css',
  styles: `
    .new-code {
      margin: 1rem 0 1.5rem;
      padding: 1rem;
      border: 1px solid var(--shop-border);
      border-radius: 1rem;
      background: var(--shop-card);
    }
    .new-code h2 {
      margin: 0 0 0.75rem;
      font-size: 1.15rem;
    }
    .fields {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(11rem, 1fr));
      gap: 0.75rem;
      margin-bottom: 0.75rem;
    }
    .form-error {
      color: var(--stock-out);
      font-weight: 600;
    }
  `,
})
export class DiscountCodesTab {
  private readonly discounts = inject(DiscountService);
  private readonly notify = inject(NotifyService);

  protected readonly codes = signal<DiscountCode[]>([]);
  protected readonly loadError = signal<string | null>(null);
  protected readonly formError = signal<string | null>(null);
  protected readonly saving = signal(false);

  protected readonly form = new FormGroup({
    code: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/^[A-Za-z0-9-]{3,20}$/)],
    }),
    percent: new FormControl<number | null>(10, {
      validators: [Validators.required, Validators.min(1), Validators.max(50)],
    }),
    validFrom: new FormControl(isoDate(), { nonNullable: true, validators: [Validators.required] }),
    validUntil: new FormControl(isoDate(new Date(Date.now() + 30 * 86400000)), {
      nonNullable: true,
      validators: [Validators.required],
    }),
    maxUses: new FormControl<number | null>(null, { validators: [Validators.min(1)] }),
  });

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loadError.set(null);
    this.discounts.list().subscribe({
      next: (list) => this.codes.set(list),
      error: (error) => this.loadError.set(userMessage(error)),
    });
  }

  protected message(field: keyof typeof this.form.controls, fallback: string): string {
    const errors = this.form.controls[field].errors ?? {};
    return typeof errors['server'] === 'string' ? errors['server'] : fallback;
  }

  protected create(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.saving.set(true);
    this.formError.set(null);
    this.discounts
      .create({
        code: value.code.trim(),
        percent: value.percent!,
        validFrom: value.validFrom,
        validUntil: value.validUntil,
        maxUses: value.maxUses || null,
      })
      .subscribe({
        next: (created) => {
          this.saving.set(false);
          this.notify.success(`Rabattcode ${created.code} angelegt`);
          this.form.controls.code.reset('');
          this.load();
        },
        error: (error) => {
          this.saving.set(false);
          const errors = fieldErrors(error);
          for (const { field, message } of errors) {
            this.form.get(field)?.setErrors({ server: message });
            this.form.get(field)?.markAsTouched();
          }
          this.formError.set(
            errors.length > 0 ? 'Bitte prüfe die markierten Felder.' : userMessage(error),
          );
        },
      });
  }

  protected toggle(code: DiscountCode): void {
    this.discounts.setActive(code.id, !code.active).subscribe({
      next: () => this.load(),
      error: (error) => this.loadError.set(userMessage(error)),
    });
  }

  protected remove(code: DiscountCode): void {
    this.discounts.delete(code.id).subscribe({
      next: () => {
        this.notify.success(`Rabattcode ${code.code} gelöscht`);
        this.load();
      },
      error: (error) => this.loadError.set(userMessage(error)),
    });
  }
}

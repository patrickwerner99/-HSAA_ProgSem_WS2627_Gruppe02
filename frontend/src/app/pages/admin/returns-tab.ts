import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { ItemCondition, ProductReturn, ReturnStatus } from '../../api/models';
import { ReturnService } from '../../api/return.service';
import { fieldErrors, userMessage } from '../../shared/errors';
import { NotifyService } from '../../shared/notify.service';
import { RETURN_STATUS_LABELS, ReturnCard } from '../../shared/return-card';

/**
 * Reiter "Retouren" der Pflegeseite (Sprint 3, F2, docs/anforderungen/0026). Ablauf:
 * Ware prüfen (je Position originalverschlossen/geöffnet) → "Eingegangen" (originalverschlossene Teile kommen als reduzierte
 * Ware in den Verkauf) → "Erstattet". Ablehnen geht bis zur Erstattung, mit Notiz für den Kunden.
 */
@Component({
  selector: 'app-returns-tab',
  imports: [FormsModule, MatButtonModule, ReturnCard],
  template: `
    <div class="filter" role="group" aria-label="Nach Status filtern">
      @for (option of filters; track option.label) {
        <button
          type="button"
          class="pill"
          [class.selected]="status() === option.value"
          (click)="setFilter(option.value)"
        >
          {{ option.label }}
        </button>
      }
    </div>

    @if (error(); as message) {
      <p class="form-error" role="alert">{{ message }}</p>
    }

    <div class="list">
      @for (r of returns(); track r.id) {
        <app-return-card [productReturn]="r" [showCustomer]="true">
          @if (r.allowedNextStatuses.length > 0) {
            <div class="admin-actions">
              @if (r.status === 'REQUESTED') {
                <p><strong>Ware prüfen:</strong></p>
                @for (line of r.lines; track line.productId) {
                  <label class="condition">
                    {{ line.productName }}:
                    <select
                      [ngModel]="conditionOf(r.id, line.productId)"
                      (ngModelChange)="setCondition(r.id, line.productId, $event)"
                    >
                      <option value="UNUSED">
                        originalverschlossen, Siegel intakt → wieder verkaufen
                      </option>
                      <option value="USED">geöffnet / beschädigt → nicht wieder verkaufen</option>
                    </select>
                  </label>
                }
              }
              <label class="note">
                Notiz für den Kunden (optional)
                <input [ngModel]="notes()[r.id] ?? ''" (ngModelChange)="setNote(r.id, $event)" />
              </label>
              <div class="buttons">
                @for (next of r.allowedNextStatuses; track next) {
                  <button
                    mat-stroked-button
                    type="button"
                    [class.danger]="next === 'REJECTED'"
                    [disabled]="saving()"
                    (click)="update(r, next)"
                  >
                    {{ actionLabel(next) }}
                  </button>
                }
              </div>
            </div>
          }
        </app-return-card>
      } @empty {
        <p>Keine Retouren{{ status() ? ' in diesem Status' : '' }}.</p>
      }
    </div>
  `,
  styles: `
    .filter {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
      margin: 1rem 0;
    }
    .pill {
      padding: 0.35rem 0.9rem;
      border: 2px solid var(--shop-dark);
      border-radius: 999px;
      background: transparent;
      font: inherit;
      font-weight: 600;
      cursor: pointer;
    }
    .pill.selected {
      background: var(--shop-dark);
      color: #fff;
    }
    .list {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }
    .admin-actions {
      margin-top: 0.75rem;
      padding-top: 0.75rem;
      border-top: 1px dashed var(--shop-border);
    }
    .condition,
    .note {
      display: block;
      margin: 0.35rem 0;
    }
    .condition select,
    .note input {
      margin-left: 0.5rem;
      padding: 0.25rem 0.5rem;
      font: inherit;
    }
    .buttons {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
      margin-top: 0.5rem;
    }
    .danger {
      color: var(--stock-out);
    }
    .form-error {
      color: var(--stock-out);
      font-weight: 600;
    }
  `,
})
export class ReturnsTab {
  private readonly service = inject(ReturnService);
  private readonly notify = inject(NotifyService);

  protected readonly filters: { value: ReturnStatus | null; label: string }[] = [
    { value: 'REQUESTED', label: 'Offen' },
    { value: 'RECEIVED', label: 'Zu erstatten' },
    { value: null, label: 'Alle' },
  ];
  protected readonly status = signal<ReturnStatus | null>('REQUESTED');
  protected readonly returns = signal<ProductReturn[]>([]);
  protected readonly error = signal<string | null>(null);
  protected readonly saving = signal(false);
  /** Zustand je Retoure und Produkt; Standard "originalverschlossen". */
  private readonly conditions = signal<Record<string, ItemCondition>>({});
  protected readonly notes = signal<Record<number, string>>({});

  constructor() {
    this.load();
  }

  protected setFilter(status: ReturnStatus | null): void {
    this.status.set(status);
    this.load();
  }

  protected conditionOf(returnId: number, productId: number): ItemCondition {
    return this.conditions()[`${returnId}-${productId}`] ?? 'UNUSED';
  }

  protected setCondition(returnId: number, productId: number, condition: ItemCondition): void {
    this.conditions.update((c) => ({ ...c, [`${returnId}-${productId}`]: condition }));
  }

  protected setNote(returnId: number, note: string): void {
    this.notes.update((n) => ({ ...n, [returnId]: note }));
  }

  protected actionLabel(next: ReturnStatus): string {
    switch (next) {
      case 'RECEIVED':
        return 'Eingegangen und geprüft';
      case 'REFUNDED':
        return 'Erstattung abschließen';
      default:
        return RETURN_STATUS_LABELS[next] === 'Abgelehnt' ? 'Ablehnen' : RETURN_STATUS_LABELS[next];
    }
  }

  protected update(r: ProductReturn, next: ReturnStatus): void {
    this.saving.set(true);
    this.error.set(null);
    this.service
      .update(r.id, {
        status: next,
        note: this.notes()[r.id] || undefined,
        ...(next === 'RECEIVED'
          ? {
              conditions: r.lines.map((line) => ({
                productId: line.productId,
                condition: this.conditionOf(r.id, line.productId),
              })),
            }
          : {}),
      })
      .subscribe({
        next: (updated) => {
          this.saving.set(false);
          this.notify.success(`Retoure #${updated.id}: ${RETURN_STATUS_LABELS[updated.status]}`);
          this.load();
        },
        error: (error) => {
          this.saving.set(false);
          this.error.set(fieldErrors(error)[0]?.message ?? userMessage(error));
        },
      });
  }

  private load(): void {
    this.service.list({ status: this.status(), page: 0 }).subscribe({
      next: (page) => this.returns.set(page.items),
      error: (error) => this.error.set(userMessage(error)),
    });
  }
}

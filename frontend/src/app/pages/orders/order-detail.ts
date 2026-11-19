import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { fieldErrors } from '../../shared/errors';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../api/auth.service';
import { Order, OrderStatus, ProductReturn } from '../../api/models';
import { ReturnService } from '../../api/return.service';
import { ReturnCard } from '../../shared/return-card';
import { OrderService } from '../../api/order.service';
import { ConfirmData, ConfirmDialog } from '../../shared/confirm-dialog';
import { userMessage } from '../../shared/errors';
import { NotifyService } from '../../shared/notify.service';
import { ORDER_STATUS_LABELS } from '../../shared/order-status';
import { StatusChip } from '../../shared/status-chip';

/**
 * /bestellungen/:id – eine Bestellung mit Positionen. Kunden können stornieren, solange sie
 * noch nicht bearbeitet wird; Admins setzen den nächsten Status (B4). Welche Knöpfe erscheinen,
 * entscheidet das Backend über allowedNextStatuses.
 */
@Component({
  selector: 'app-order-detail',
  imports: [
    CurrencyPipe,
    DatePipe,
    RouterLink,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule,
    StatusChip,
    ReturnCard,
  ],
  templateUrl: './order-detail.html',
  styleUrl: './order-detail.css',
})
export class OrderDetailPage {
  private readonly orders = inject(OrderService);
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotifyService);
  private readonly returnService = inject(ReturnService);

  readonly id = input.required<string>();

  protected readonly labels = ORDER_STATUS_LABELS;
  protected readonly isAdmin = this.auth.isAdmin;
  protected readonly order = signal<Order | null>(null);
  protected readonly loading = signal(true);
  protected readonly notFound = signal(false);
  protected readonly loadError = signal<string | null>(null);
  protected readonly saving = signal(false);

  protected readonly canCancel = computed(
    () => !this.isAdmin() && !!this.order()?.allowedNextStatuses.includes('CANCELLED'),
  );
  protected readonly backLink = computed(() =>
    this.isAdmin() ? '/pflege/bestellungen' : '/bestellungen',
  );

  // ---- Retouren (F1) ----
  protected readonly returns = signal<ProductReturn[]>([]);
  protected readonly returnFormOpen = signal(false);
  /** Gewünschte Menge je Produkt-ID im Retourenformular. */
  protected readonly returnQuantities = signal<Record<number, number>>({});
  protected readonly returnReason = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(500)],
  });
  protected readonly returnError = signal<string | null>(null);
  protected readonly sendingReturn = signal(false);

  /** Wie viele Stück je Produkt noch zurückgeschickt werden können (abgelehnte Retouren zählen nicht). */
  protected readonly returnable = computed(() => {
    const left: Record<number, number> = {};
    for (const line of this.order()?.lines ?? []) {
      left[line.productId] = (left[line.productId] ?? 0) + line.quantity;
    }
    for (const r of this.returns()) {
      if (r.status === 'REJECTED') {
        continue;
      }
      for (const line of r.lines) {
        left[line.productId] = (left[line.productId] ?? 0) - line.quantity;
      }
    }
    return left;
  });
  protected readonly canReturn = computed(
    () =>
      !this.isAdmin() &&
      this.order()?.status === 'DELIVERED' &&
      Object.values(this.returnable()).some((left) => left > 0),
  );

  constructor() {
    effect(() => this.load(this.id()));
  }

  protected openReturnForm(): void {
    this.returnQuantities.set({});
    this.returnReason.reset('');
    this.returnError.set(null);
    this.returnFormOpen.set(true);
  }

  protected setReturnQuantity(productId: number, value: string): void {
    const max = this.returnable()[productId] ?? 0;
    const quantity = Math.max(0, Math.min(max, Math.floor(Number(value) || 0)));
    this.returnQuantities.update((current) => ({ ...current, [productId]: quantity }));
  }

  protected sendReturn(): void {
    const order = this.order();
    const items = Object.entries(this.returnQuantities())
      .filter(([, quantity]) => quantity > 0)
      .map(([productId, quantity]) => ({ productId: Number(productId), quantity }));
    if (!order || this.sendingReturn()) {
      return;
    }
    if (items.length === 0) {
      this.returnError.set('Bitte wähle mindestens ein Produkt und eine Menge.');
      return;
    }
    if (this.returnReason.invalid) {
      this.returnReason.markAsTouched();
      this.returnError.set('Bitte gib einen Grund an.');
      return;
    }
    this.sendingReturn.set(true);
    this.returnError.set(null);
    this.returnService
      .create({ orderId: order.id, reason: this.returnReason.value.trim(), items })
      .subscribe({
        next: () => {
          this.sendingReturn.set(false);
          this.returnFormOpen.set(false);
          this.notify.success('Deine Retoure ist angemeldet. Den Stand siehst du hier jederzeit.');
          this.loadReturns(order.id);
        },
        error: (error) => {
          this.sendingReturn.set(false);
          const errors = fieldErrors(error);
          this.returnError.set(errors[0]?.message ?? userMessage(error));
        },
      });
  }

  private loadReturns(orderId: number): void {
    this.returnService.forOrder(orderId).subscribe({
      next: (list) => this.returns.set(list),
      error: () => this.returns.set([]),
    });
  }

  protected retry(): void {
    this.load(this.id());
  }

  protected cancel(): void {
    const order = this.order();
    if (!order) {
      return;
    }
    const data: ConfirmData = {
      title: 'Bestellung stornieren?',
      message: `Möchtest du Bestellung #${order.id} wirklich stornieren?`,
      confirmLabel: 'Stornieren',
    };
    this.dialog
      .open(ConfirmDialog, { data })
      .afterClosed()
      .subscribe((confirmed) => confirmed && this.setStatus('CANCELLED'));
  }

  protected setStatus(status: OrderStatus): void {
    const order = this.order();
    if (!order || this.saving()) {
      return;
    }
    this.saving.set(true);
    this.orders.changeStatus(order.id, status).subscribe({
      next: (updated) => {
        this.saving.set(false);
        this.order.set(updated);
        this.notify.success(`Status ist jetzt „${ORDER_STATUS_LABELS[updated.status]}“.`);
      },
      error: (error) => {
        this.saving.set(false);
        this.notify.error(userMessage(error));
        // Vielleicht hat jemand anderes den Status inzwischen geändert → aktuellen Stand zeigen.
        if (error instanceof HttpErrorResponse && error.status === 409) {
          this.load(this.id());
        }
      },
    });
  }

  private load(rawId: string): void {
    this.notFound.set(false);
    this.loadError.set(null);
    const id = Number(rawId);
    if (!Number.isInteger(id) || id <= 0) {
      this.loading.set(false);
      this.notFound.set(true);
      return;
    }
    this.loading.set(this.order()?.id !== id);
    this.orders.get(id).subscribe({
      next: (order) => {
        this.order.set(order);
        this.loading.set(false);
        if (order.status === 'DELIVERED') {
          this.loadReturns(order.id);
        }
      },
      error: (error) => {
        this.loading.set(false);
        this.order.set(null);
        if (error instanceof HttpErrorResponse && (error.status === 404 || error.status === 400)) {
          this.notFound.set(true);
        } else {
          this.loadError.set(userMessage(error));
        }
      },
    });
  }
}

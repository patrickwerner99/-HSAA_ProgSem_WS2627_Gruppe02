import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../api/auth.service';
import { Order, OrderStatus } from '../../api/models';
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
    StatusChip,
  ],
  templateUrl: './order-detail.html',
  styleUrl: './order-detail.css',
})
export class OrderDetailPage {
  private readonly orders = inject(OrderService);
  private readonly auth = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotifyService);

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

  constructor() {
    effect(() => this.load(this.id()));
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

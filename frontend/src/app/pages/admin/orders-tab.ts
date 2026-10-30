import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { Order, OrderStatus } from '../../api/models';
import { ORDER_PAGE_SIZE, OrderService } from '../../api/order.service';
import { userMessage } from '../../shared/errors';
import { NotifyService } from '../../shared/notify.service';
import {
  ORDER_STATUSES,
  ORDER_STATUS_LABELS,
  isOrderStatus,
  pageFromUrl,
} from '../../shared/order-status';
import { Pager } from '../../shared/pager';
import { StatusChip } from '../../shared/status-chip';

/**
 * Reiter "Bestellungen" (B4, docs/anforderungen/0011): alle Bestellungen, Filter und Seite in der URL
 * (/pflege/bestellungen?status=RECEIVED&seite=2). Im Status-Menü stehen nur die Übergänge,
 * die das Backend erlaubt (allowedNextStatuses).
 */
@Component({
  selector: 'app-orders-tab',
  imports: [
    CurrencyPipe,
    DatePipe,
    RouterLink,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    Pager,
    StatusChip,
  ],
  template: `
    <div class="pills" role="group" aria-label="Nach Status filtern">
      <button
        type="button"
        class="pill"
        [class.selected]="status() === null"
        (click)="filter(null)"
      >
        Alle
      </button>
      @for (value of statuses; track value) {
        <button
          type="button"
          class="pill"
          [class.selected]="status() === value"
          [attr.aria-pressed]="status() === value"
          (click)="filter(value)"
        >
          {{ labels[value] }}
        </button>
      }
    </div>

    @if (loadError(); as message) {
      <div class="state-message error" role="alert">
        <p>Die Bestellungen konnten nicht geladen werden.</p>
        <p>{{ message }}</p>
        <button mat-flat-button type="button" (click)="load()">Erneut versuchen</button>
      </div>
    } @else if (loading() && items().length === 0) {
      <div class="state-message"><mat-spinner diameter="36" /></div>
    } @else if (items().length === 0) {
      <div class="state-message">
        <p>{{ status() ? 'Keine Bestellungen mit diesem Status.' : 'Noch keine Bestellungen.' }}</p>
      </div>
    } @else {
      <p class="hint">{{ total() }} {{ total() === 1 ? 'Bestellung' : 'Bestellungen' }}</p>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th scope="col">Nr.</th>
              <th scope="col">Datum</th>
              <th scope="col">Kunde</th>
              <th scope="col" class="num">Artikel</th>
              <th scope="col" class="num">Summe</th>
              <th scope="col">Status</th>
              <th scope="col">Ändern</th>
            </tr>
          </thead>
          <tbody>
            @for (order of items(); track order.id) {
              <tr>
                <td>
                  <a [routerLink]="['/bestellungen', order.id]">#{{ order.id }}</a>
                </td>
                <td>{{ order.createdAt | date: 'dd.MM.yy, HH:mm' }}</td>
                <td class="name">
                  {{ order.customer.name }}
                  <span class="email">{{ order.customer.email }}</span>
                </td>
                <td class="num">{{ order.itemCount }}</td>
                <td class="num">{{ order.total | currency }}</td>
                <td><app-status-chip [status]="order.status" /></td>
                <td>
                  @if (order.allowedNextStatuses.length > 0) {
                    <mat-select
                      class="status-select"
                      placeholder="Status setzen"
                      [attr.aria-label]="'Status von Bestellung ' + order.id + ' ändern'"
                      [disabled]="saving() === order.id"
                      (selectionChange)="change(order, $event.value)"
                    >
                      @for (next of order.allowedNextStatuses; track next) {
                        <mat-option [value]="next">{{ labels[next] }}</mat-option>
                      }
                    </mat-select>
                  } @else {
                    <span class="hint">abgeschlossen</span>
                  }
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>
      <app-pager [page]="page()" [totalItems]="total()" [size]="pageSize" (go)="go($event)" />
    }
  `,
  styleUrl: './admin-table.css',
  styles: `
    .pills {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
      margin: 1.25rem 0 1rem;
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
      color: var(--shop-accent);
    }
    .email {
      display: block;
      font-weight: 400;
      font-size: 0.85rem;
      color: var(--shop-muted);
    }
    .status-select {
      width: 11rem;
      padding: 0.3rem 0.6rem;
      border: 1px solid var(--shop-border);
      border-radius: 0.5rem;
      background: #fff;
    }
    .hint {
      margin: 0 0 0.75rem;
    }
  `,
})
export class OrdersTab {
  private readonly orders = inject(OrderService);
  private readonly notify = inject(NotifyService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly statuses = ORDER_STATUSES;
  protected readonly labels = ORDER_STATUS_LABELS;
  protected readonly pageSize = ORDER_PAGE_SIZE;

  protected readonly items = signal<Order[]>([]);
  protected readonly total = signal(0);
  protected readonly page = signal(0);
  protected readonly status = signal<OrderStatus | null>(null);
  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  /** ID der Bestellung, deren Status gerade gespeichert wird. */
  protected readonly saving = signal<number | null>(null);

  private request?: Subscription;

  constructor() {
    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      const status = params.get('status');
      this.status.set(isOrderStatus(status) ? status : null);
      this.page.set(pageFromUrl(params.get('seite')));
      this.load();
    });
  }

  protected filter(status: OrderStatus | null): void {
    this.updateUrl({ status, seite: null });
  }

  protected go(page: number): void {
    this.updateUrl({ seite: page > 0 ? page + 1 : null });
  }

  protected change(order: Order, status: OrderStatus): void {
    this.saving.set(order.id);
    this.orders.changeStatus(order.id, status).subscribe({
      next: (updated) => {
        this.saving.set(null);
        this.notify.success(`Bestellung #${updated.id}: „${ORDER_STATUS_LABELS[updated.status]}“`);
        this.load();
      },
      error: (error) => {
        this.saving.set(null);
        this.notify.error(userMessage(error));
        this.load();
      },
    });
  }

  protected load(): void {
    this.request?.unsubscribe();
    this.loading.set(true);
    this.loadError.set(null);
    this.request = this.orders.list({ status: this.status(), page: this.page() }).subscribe({
      next: (result) => {
        // Seite gibt es nach einem Filterwechsel nicht mehr → auf die letzte vorhandene springen.
        if (result.items.length === 0 && result.totalItems > 0 && this.page() > 0) {
          this.go(Math.ceil(result.totalItems / ORDER_PAGE_SIZE) - 1);
          return;
        }
        this.items.set(result.items);
        this.total.set(result.totalItems);
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.loadError.set(userMessage(error));
      },
    });
  }

  private updateUrl(changes: Record<string, string | number | null>): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: changes,
      queryParamsHandling: 'merge',
    });
  }
}

import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { Order, OrderStatus } from '../../api/models';
import { ORDER_PAGE_SIZE, OrderService } from '../../api/order.service';
import { userMessage } from '../../shared/errors';
import {
  ORDER_STATUSES,
  ORDER_STATUS_LABELS,
  isOrderStatus,
  pageFromUrl,
} from '../../shared/order-status';
import { Pager } from '../../shared/pager';
import { StatusChip } from '../../shared/status-chip';

/** /bestellungen?status=…&seite=… – eigene Bestellungen wiederfinden (A1, docs/anforderungen/0010). */
@Component({
  selector: 'app-my-orders',
  imports: [
    CurrencyPipe,
    DatePipe,
    RouterLink,
    MatButtonModule,
    MatProgressSpinnerModule,
    Pager,
    StatusChip,
  ],
  templateUrl: './my-orders.html',
  styleUrl: './orders.css',
})
export class MyOrdersPage {
  private readonly orders = inject(OrderService);
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

  protected load(): void {
    this.request?.unsubscribe();
    this.loading.set(true);
    this.loadError.set(null);
    this.request = this.orders.list({ status: this.status(), page: this.page() }).subscribe({
      next: (result) => {
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

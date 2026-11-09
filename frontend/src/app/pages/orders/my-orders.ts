import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Order } from '../../api/models';
import { ORDER_PAGE_SIZE, OrderService } from '../../api/order.service';
import { userMessage } from '../../shared/errors';
import { pageFromUrl } from '../../shared/order-status';
import { Pager } from '../../shared/pager';
import { StatusChip } from '../../shared/status-chip';

/** /bestellungen?seite=… – eigene Bestellungen wiederfinden, neueste zuerst (A1, docs/anforderungen/0010). */
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

  protected readonly pageSize = ORDER_PAGE_SIZE;
  protected readonly items = signal<Order[]>([]);
  protected readonly total = signal(0);
  protected readonly page = signal(0);
  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);

  constructor() {
    // Die Seite steht in der URL, damit ein Link genau diese Seite zeigt (C2).
    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      this.page.set(pageFromUrl(params.get('seite')));
      this.load();
    });
  }

  protected go(page: number): void {
    this.router.navigate([], { queryParams: { seite: page > 0 ? page + 1 : null } });
  }

  protected load(): void {
    this.loading.set(true);
    this.loadError.set(null);
    this.orders.list({ page: this.page() }).subscribe({
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
}

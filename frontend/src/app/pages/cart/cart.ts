import { CurrencyPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router, RouterLink } from '@angular/router';
import { Observable, Subscription, catchError, forkJoin, map, of, throwError } from 'rxjs';
import { AuthService } from '../../api/auth.service';
import { Product, StockProblem } from '../../api/models';
import { OrderService } from '../../api/order.service';
import { ProductService } from '../../api/product.service';
import { CartService, MAX_QUANTITY } from '../../shared/cart.service';
import { userMessage } from '../../shared/errors';
import { FallbackImageDirective } from '../../shared/fallback-image.directive';
import { PLACEHOLDER_IMAGE } from '../../shared/format';
import { NotifyService } from '../../shared/notify.service';

export interface CartLine {
  productId: number;
  quantity: number;
  /** null = Produkt gibt es nicht mehr. */
  product: Product | null;
  /** Hinweis, warum diese Position so nicht bestellt werden kann. */
  problem: string | null;
}

/**
 * /warenkorb (docs/anforderungen/0008, 0009). Preise und Bestand kommen immer frisch vom Shop,
 * im Browser liegen nur Produkt-ID und Menge (ADR-0005). Anmelden erst beim Bestellen.
 */
@Component({
  selector: 'app-cart',
  imports: [
    CurrencyPipe,
    RouterLink,
    MatButtonModule,
    MatProgressSpinnerModule,
    FallbackImageDirective,
  ],
  templateUrl: './cart.html',
  styleUrl: './cart.css',
})
export class CartPage {
  private readonly cart = inject(CartService);
  private readonly products = inject(ProductService);
  private readonly orders = inject(OrderService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notify = inject(NotifyService);

  protected readonly placeholder = PLACEHOLDER_IMAGE;
  protected readonly maxQuantity = MAX_QUANTITY;
  protected readonly loggedIn = this.auth.loggedIn;

  /** Aktuelle Produktdaten vom Shop, nach ID. */
  private readonly loaded = signal<Map<number, Product | null>>(new Map());
  protected readonly loading = signal(true);
  protected readonly loadError = signal<string | null>(null);
  protected readonly ordering = signal(false);
  /** Meldung nach einer abgelehnten Bestellung (z. B. Bestand hat sich geändert). */
  protected readonly orderError = signal<string | null>(null);

  protected readonly lines = computed<CartLine[]>(() =>
    this.cart.items().map(({ productId, quantity }) => {
      const product = this.loaded().get(productId);
      return {
        productId,
        quantity,
        product: product ?? null,
        problem: problemFor(product, quantity),
      };
    }),
  );
  /** Nur zur Anzeige. Den verbindlichen Betrag rechnet das Backend beim Bestellen (B3). */
  protected readonly total = computed(() =>
    this.lines().reduce(
      (sum, line) => sum + (line.product ? line.product.price * line.quantity : 0),
      0,
    ),
  );
  protected readonly hasProblems = computed(() => this.lines().some((line) => line.problem));

  private request?: Subscription;

  constructor() {
    this.reload();
    inject(DestroyRef).onDestroy(() => this.request?.unsubscribe());
  }

  protected reload(): void {
    this.request?.unsubscribe();
    const ids = this.cart.items().map((item) => item.productId);
    this.loading.set(true);
    this.loadError.set(null);
    if (ids.length === 0) {
      this.loaded.set(new Map());
      this.loading.set(false);
      return;
    }
    this.request = forkJoin(ids.map((id) => this.loadProduct(id))).subscribe({
      next: (list) => {
        this.loaded.set(new Map(ids.map((id, index) => [id, list[index]])));
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.loadError.set(userMessage(error));
      },
    });
  }

  protected change(line: CartLine, delta: number): void {
    this.orderError.set(null);
    this.cart.set(line.productId, line.quantity + delta);
  }

  /** Menge auf den verfügbaren Bestand senken. */
  protected fit(line: CartLine): void {
    this.orderError.set(null);
    this.cart.set(line.productId, line.product?.stock ?? 0);
  }

  protected remove(line: CartLine): void {
    this.orderError.set(null);
    this.cart.remove(line.productId);
  }

  protected order(): void {
    if (this.ordering() || this.hasProblems() || this.lines().length === 0) {
      return;
    }
    if (!this.loggedIn()) {
      this.router.navigate(['/anmelden'], { queryParams: { zurueck: '/warenkorb' } });
      return;
    }
    this.ordering.set(true);
    this.orderError.set(null);
    this.orders.place(this.cart.items()).subscribe({
      next: (order) => {
        this.cart.clear();
        this.notify.success('Danke! Deine Bestellung ist eingegangen.');
        this.router.navigate(['/bestellungen', order.id]);
      },
      error: (error) => {
        this.ordering.set(false);
        if (error instanceof HttpErrorResponse && error.status === 401) {
          this.router.navigate(['/anmelden'], { queryParams: { zurueck: '/warenkorb' } });
          return;
        }
        if (error instanceof HttpErrorResponse && error.status === 409) {
          // Bestand hat sich seit dem Laden geändert (B2): neu laden, Hinweise stehen dann an den Positionen.
          const problems: StockProblem[] = error.error?.items ?? [];
          this.orderError.set(
            problems.length > 0
              ? 'Inzwischen hat sich der Bestand geändert. Bitte passe die markierten Produkte an.'
              : userMessage(error),
          );
          this.reload();
          return;
        }
        this.orderError.set(userMessage(error));
      },
    });
  }

  /** 404 = Produkt wurde gelöscht → null; andere Fehler brechen das Laden ab. */
  private loadProduct(id: number): Observable<Product | null> {
    return this.products.get(id).pipe(
      map((product): Product | null => product),
      catchError((error) =>
        error instanceof HttpErrorResponse && error.status === 404
          ? of(null)
          : throwError(() => error),
      ),
    );
  }
}

export function problemFor(product: Product | null | undefined, quantity: number): string | null {
  if (product === undefined) {
    return null; // noch nicht geladen
  }
  if (product === null) {
    return 'Dieses Produkt gibt es nicht mehr.';
  }
  if (product.stock === 0) {
    return 'Ausverkauft.';
  }
  if (product.stock < quantity) {
    return `Nur noch ${product.stock} verfügbar.`;
  }
  return null;
}

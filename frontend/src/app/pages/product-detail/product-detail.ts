import { CurrencyPipe, Location } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, effect, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { Product } from '../../api/models';
import { ProductService } from '../../api/product.service';
import { userMessage } from '../../shared/errors';
import { FallbackImageDirective } from '../../shared/fallback-image.directive';
import { PLACEHOLDER_IMAGE, stockView } from '../../shared/format';

/** Detailseite /produkte/:id mit Bestandsstatus (docs/anforderungen/0002). */
@Component({
  selector: 'app-product-detail',
  imports: [
    CurrencyPipe,
    RouterLink,
    MatButtonModule,
    MatProgressSpinnerModule,
    FallbackImageDirective,
  ],
  templateUrl: './product-detail.html',
  styleUrl: './product-detail.css',
})
export class ProductDetailPage {
  private readonly productService = inject(ProductService);
  private readonly title = inject(Title);
  private readonly location = inject(Location);

  /** Kommt aus der URL /produkte/:id (withComponentInputBinding). */
  readonly id = input.required<string>();

  protected readonly placeholder = PLACEHOLDER_IMAGE;
  protected readonly product = signal<Product | null>(null);
  protected readonly loading = signal(true);
  protected readonly notFound = signal(false);
  protected readonly loadError = signal<string | null>(null);
  protected readonly stock = computed(() => {
    const product = this.product();
    return product ? stockView(product.stockStatus) : null;
  });

  private request?: Subscription;

  constructor() {
    effect(() => this.load(this.id()));
    inject(DestroyRef).onDestroy(() => this.request?.unsubscribe());
  }

  protected retry(): void {
    this.load(this.id());
  }

  /** Zurück zur Liste mit altem Filter und alter Scroll-Position; ohne Verlauf zur Startseite. */
  protected back(event: Event): void {
    if (window.history.length > 1) {
      event.preventDefault();
      this.location.back();
    }
  }

  private load(rawId: string): void {
    this.request?.unsubscribe();
    this.product.set(null);
    this.notFound.set(false);
    this.loadError.set(null);

    // /produkte/abc → gleiche Meldung wie bei einem unbekannten Produkt, kein technischer Fehler.
    const id = Number(rawId);
    if (!Number.isInteger(id) || id <= 0) {
      this.loading.set(false);
      this.notFound.set(true);
      return;
    }

    this.loading.set(true);
    this.request = this.productService.get(id).subscribe({
      next: (product) => {
        this.product.set(product);
        this.loading.set(false);
        this.title.setTitle(`${product.name} | Hyrox-Shop`);
      },
      error: (error) => {
        this.loading.set(false);
        if (error instanceof HttpErrorResponse && (error.status === 404 || error.status === 400)) {
          this.notFound.set(true);
        } else {
          this.loadError.set(userMessage(error));
        }
      },
    });
  }
}

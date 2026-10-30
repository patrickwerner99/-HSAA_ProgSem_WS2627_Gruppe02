import { Component, computed, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';
import { Router } from '@angular/router';
import { EMPTY, expand, forkJoin, reduce } from 'rxjs';
import { CategoryService } from '../../api/category.service';
import { Category, Product } from '../../api/models';
import { ProductService } from '../../api/product.service';
import { userMessage } from '../../shared/errors';
import { CategoriesTab } from './categories-tab';
import { OrdersTab } from './orders-tab';
import { ProductsTab } from './products-tab';

/** Reiter der Pflegeseite; jeder hat eine eigene Adresse, z. B. /pflege/bestellungen (C2). */
export const ADMIN_TABS = ['produkte', 'kategorien', 'bestellungen'] as const;
export type AdminTab = (typeof ADMIN_TABS)[number];

/**
 * Pflegeseite /pflege/:reiter mit Produkten, Kategorien und Bestellungen (docs/anforderungen/0003, 0011).
 * Nur für Admins (adminGuard); die eigentliche Sperre sitzt im Backend (A2).
 */
@Component({
  selector: 'app-admin',
  imports: [
    MatTabsModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    ProductsTab,
    CategoriesTab,
    OrdersTab,
  ],
  template: `
    <h1>Pflege</h1>

    @if (loadError(); as message) {
      <div class="state-message error" role="alert">
        <p>Die Daten konnten nicht geladen werden.</p>
        <p>{{ message }}</p>
        <button mat-flat-button type="button" (click)="reload()">Erneut versuchen</button>
      </div>
    } @else if (loading() && products().length === 0 && categories().length === 0) {
      <div class="state-message"><mat-spinner diameter="36" /></div>
    } @else {
      <mat-tab-group
        animationDuration="0ms"
        [selectedIndex]="tabIndex()"
        (selectedIndexChange)="openTab($event)"
      >
        <mat-tab label="Produkte ({{ products().length }})">
          <app-products-tab
            [products]="products()"
            [categories]="categories()"
            (changed)="reload()"
          />
        </mat-tab>
        <mat-tab label="Kategorien ({{ categories().length }})">
          <app-categories-tab [categories]="categories()" (changed)="reload()" />
        </mat-tab>
        <mat-tab label="Bestellungen">
          <ng-template matTabContent>
            <app-orders-tab />
          </ng-template>
        </mat-tab>
      </mat-tab-group>
    }
  `,
})
export class AdminPage {
  private readonly productService = inject(ProductService);
  private readonly categoryService = inject(CategoryService);
  private readonly router = inject(Router);

  /** Aus der URL /pflege/:reiter (withComponentInputBinding). */
  readonly reiter = input<string>();
  protected readonly tabIndex = computed(() =>
    Math.max(0, ADMIN_TABS.indexOf(this.reiter() as AdminTab)),
  );

  protected readonly products = signal<Product[]>([]);
  protected readonly categories = signal<Category[]>([]);
  protected readonly loading = signal(false);
  protected readonly loadError = signal<string | null>(null);

  constructor() {
    this.reload();
  }

  protected openTab(index: number): void {
    // Neuer Reiter → Filter des alten Reiters (z. B. ?status=…) gehören nicht mehr dazu.
    this.router.navigate(['/pflege', ADMIN_TABS[index]]);
  }

  /** Lädt alle Produkte (seitenweise, je 50) und alle Kategorien neu. */
  protected reload(): void {
    this.loading.set(true);
    this.loadError.set(null);

    const allProducts$ = this.productService.search({ page: 0, size: 50 }).pipe(
      expand((page) =>
        page.last ? EMPTY : this.productService.search({ page: page.page + 1, size: 50 }),
      ),
      reduce((all, page) => [...all, ...page.items], [] as Product[]),
    );

    forkJoin({ products: allProducts$, categories: this.categoryService.list() }).subscribe({
      next: ({ products, categories }) => {
        this.products.set(products);
        this.categories.set(categories);
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.loadError.set(userMessage(error));
      },
    });
  }
}

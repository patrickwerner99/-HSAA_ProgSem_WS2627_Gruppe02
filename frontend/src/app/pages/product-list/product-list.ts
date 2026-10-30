import { CurrencyPipe } from '@angular/common';
import {
  Component,
  DestroyRef,
  ElementRef,
  afterNextRender,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { ActivatedRoute, ParamMap, Router, RouterLink } from '@angular/router';
import { Subscription, debounceTime, distinctUntilChanged, map } from 'rxjs';
import { CategoryService } from '../../api/category.service';
import { Category, Product, ProductSort } from '../../api/models';
import { ProductService } from '../../api/product.service';
import { userMessage } from '../../shared/errors';
import { FallbackImageDirective } from '../../shared/fallback-image.directive';
import { PLACEHOLDER_IMAGE } from '../../shared/format';
import { ProductListStore } from './product-list.store';

/** Sortierungen in der Auswahl; "name" ist Standard und steht deshalb nicht in der URL. */
export const SORT_OPTIONS: { value: ProductSort; label: string }[] = [
  { value: 'name', label: 'Name (A–Z)' },
  { value: 'price-asc', label: 'Preis aufsteigend' },
  { value: 'price-desc', label: 'Preis absteigend' },
  { value: 'newest', label: 'Neuheiten zuerst' },
];

export function sortFromUrl(value: string | null): ProductSort {
  return SORT_OPTIONS.some((option) => option.value === value) ? (value as ProductSort) : 'name';
}

/**
 * Startseite: alle Produkte als Kacheln mit Suche, Filtern, Sortierung und endlosem Scrollen
 * (docs/anforderungen/0001, 0012, ADR-0002). Alles, was die Ansicht bestimmt, steht in der URL
 * (?q=…&kategorie=…&sort=price-asc&verfuegbar=ja), damit ein gespeicherter Link gleich aussieht (C2).
 */
@Component({
  selector: 'app-product-list',
  imports: [
    CurrencyPipe,
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatSlideToggleModule,
    FallbackImageDirective,
  ],
  templateUrl: './product-list.html',
  styleUrl: './product-list.css',
})
export class ProductListPage {
  private readonly productService = inject(ProductService);
  private readonly categoryService = inject(CategoryService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly store = inject(ProductListStore);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly placeholder = PLACEHOLDER_IMAGE;
  protected readonly search = new FormControl('', { nonNullable: true });

  protected readonly categories = signal<Category[]>([]);
  protected readonly items = signal<Product[]>([]);
  protected readonly total = signal(0);
  protected readonly last = signal(false);
  protected readonly loading = signal(false);
  /** Fehler beim ersten Laden: Meldung statt Liste. */
  protected readonly loadError = signal<string | null>(null);
  /** Fehler beim Nachladen: bisherige Produkte bleiben sichtbar. */
  protected readonly moreError = signal<string | null>(null);
  protected readonly query = signal('');
  protected readonly selectedCategory = signal<number | null>(null);
  protected readonly sort = signal<ProductSort>('name');
  protected readonly inStockOnly = signal(false);
  protected readonly sortOptions = SORT_OPTIONS;
  protected readonly filterActive = computed(
    () => this.query() !== '' || this.selectedCategory() !== null || this.inStockOnly(),
  );

  private readonly sentinel = viewChild.required<ElementRef<HTMLElement>>('sentinel');
  private page = 0;
  private key = '';
  private request?: Subscription;

  constructor() {
    this.categoryService
      .list()
      .pipe(takeUntilDestroyed())
      // Ohne Kategorien funktioniert die Liste trotzdem, deshalb hier keine eigene Fehlermeldung.
      .subscribe({
        next: (list) => this.categories.set(list),
        error: () => this.categories.set([]),
      });

    this.route.queryParamMap
      .pipe(takeUntilDestroyed())
      .subscribe((params) => this.applyQuery(params));

    // Suche erst, wenn 300 ms nicht getippt wurde, sonst gäbe es eine Anfrage pro Buchstabe.
    this.search.valueChanges
      .pipe(
        debounceTime(300),
        map((value) => value.trim()),
        distinctUntilChanged(),
        takeUntilDestroyed(),
      )
      .subscribe((q) => this.updateUrl({ q: q || null }));

    afterNextRender(() => this.watchSentinel());

    this.destroyRef.onDestroy(() => {
      this.request?.unsubscribe();
      this.store.save({
        key: this.key,
        items: this.items(),
        page: this.page,
        last: this.last(),
        total: this.total(),
        scrollY: window.scrollY,
      });
    });
  }

  protected selectCategory(id: number | null): void {
    this.updateUrl({ kategorie: id });
  }

  protected selectSort(sort: ProductSort): void {
    this.updateUrl({ sort: sort === 'name' ? null : sort });
  }

  protected toggleInStock(only: boolean): void {
    this.updateUrl({ verfuegbar: only ? 'ja' : null });
  }

  protected resetFilters(): void {
    this.search.setValue('', { emitEvent: false });
    this.updateUrl({ q: null, kategorie: null, verfuegbar: null });
  }

  protected retry(): void {
    this.loadPage(this.items().length === 0 ? 0 : this.page + 1);
  }

  private applyQuery(params: ParamMap): void {
    const q = (params.get('q') ?? '').trim();
    const kategorie = Number(params.get('kategorie'));
    const categoryId = Number.isInteger(kategorie) && kategorie > 0 ? kategorie : null;

    const sort = sortFromUrl(params.get('sort'));
    const inStockOnly = params.get('verfuegbar') === 'ja';

    this.query.set(q);
    this.selectedCategory.set(categoryId);
    this.sort.set(sort);
    this.inStockOnly.set(inStockOnly);
    if (this.search.value !== q) {
      this.search.setValue(q, { emitEvent: false });
    }

    this.key = `${q}|${categoryId ?? ''}|${sort}|${inStockOnly}`;
    const snapshot = this.store.restore(this.key);
    if (snapshot) {
      this.items.set(snapshot.items);
      this.page = snapshot.page;
      this.last.set(snapshot.last);
      this.total.set(snapshot.total);
      setTimeout(() => window.scrollTo(0, snapshot.scrollY));
      return;
    }
    this.items.set([]);
    this.last.set(false);
    this.loadPage(0);
  }

  private loadPage(page: number): void {
    this.request?.unsubscribe();
    this.loading.set(true);
    this.loadError.set(null);
    this.moreError.set(null);

    this.request = this.productService
      .search({
        q: this.query(),
        categoryId: this.selectedCategory(),
        sort: this.sort(),
        inStock: this.inStockOnly(),
        page,
      })
      .subscribe({
        next: (result) => {
          this.items.update((current) =>
            page === 0 ? result.items : [...current, ...result.items],
          );
          this.page = result.page;
          this.last.set(result.last);
          this.total.set(result.totalItems);
          this.loading.set(false);
          this.loadMoreIfSentinelVisible();
        },
        error: (error) => {
          this.loading.set(false);
          if (page === 0) {
            this.loadError.set(userMessage(error));
          } else {
            this.moreError.set(userMessage(error));
          }
        },
      });
  }

  private loadMore(): void {
    if (this.loading() || this.last() || this.loadError() || this.moreError()) {
      return;
    }
    this.loadPage(this.page + 1);
  }

  /** Lädt nach, sobald das unsichtbare Element unter der Liste in Sichtweite kommt. */
  private watchSentinel(): void {
    if (typeof IntersectionObserver === 'undefined') {
      return;
    }
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((entry) => entry.isIntersecting)) {
          this.loadMore();
        }
      },
      { rootMargin: '300px' },
    );
    observer.observe(this.sentinel().nativeElement);
    this.destroyRef.onDestroy(() => observer.disconnect());
  }

  /**
   * Füllen die ersten 12 Produkte den Bildschirm nicht (großer Monitor), meldet der
   * IntersectionObserver kein neues Ereignis. Dann hier selbst prüfen und nachladen.
   */
  private loadMoreIfSentinelVisible(): void {
    setTimeout(() => {
      const top = this.sentinel().nativeElement.getBoundingClientRect().top;
      if (top < window.innerHeight + 300) {
        this.loadMore();
      }
    });
  }

  private updateUrl(changes: Record<string, string | number | null>): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: changes,
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }
}

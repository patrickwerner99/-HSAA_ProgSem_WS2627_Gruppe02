import { CurrencyPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatRadioModule } from '@angular/material/radio';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Router, RouterLink } from '@angular/router';
import { Observable, catchError, forkJoin, map, of, throwError } from 'rxjs';
import { AccountService } from '../../api/account.service';
import { AuthService } from '../../api/auth.service';
import {
  DeliveryAddress,
  DiscountCheck,
  Product,
  SavedAddress,
  StockProblem,
} from '../../api/models';
import { DiscountService } from '../../api/discount.service';
import { AddressChoice, OrderService } from '../../api/order.service';
import { ProductService } from '../../api/product.service';
import { CartService, MAX_QUANTITY } from '../../shared/cart.service';
import { fieldErrors, userMessage } from '../../shared/errors';
import { FallbackImageDirective } from '../../shared/fallback-image.directive';
import { PLACEHOLDER_IMAGE } from '../../shared/format';
import { NotifyService } from '../../shared/notify.service';
import { displayName } from '../../shared/product-form';
import { AddressFields } from '../../shared/address-fields';
import {
  addressLine,
  addressValue,
  applyAddressErrors,
  createAddressForm,
} from '../../shared/address-form';

export interface CartLine {
  productId: number;
  quantity: number;
  /** null = Produkt gibt es nicht mehr. */
  product: Product | null;
  /** Hinweis, warum diese Position so nicht bestellt werden kann. */
  problem: string | null;
}

/**
 * /warenkorb (docs/anforderungen/0008, 0009, 0017). Preise und Bestand kommen immer frisch vom Shop,
 * im Browser liegen nur Produkt-ID und Menge (ADR-0005). Anmelden erst beim Bestellen.
 * Angemeldete Kunden geben vor dem Bestellen die Lieferadresse an; sie ist mit der Adresse
 * der letzten Bestellung vorausgefüllt (ADR-0009).
 */
@Component({
  selector: 'app-cart',
  imports: [
    CurrencyPipe,
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatRadioModule,
    AddressFields,
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
  private readonly account = inject(AccountService);
  private readonly discounts = inject(DiscountService);

  protected readonly placeholder = PLACEHOLDER_IMAGE;
  protected readonly maxQuantity = MAX_QUANTITY;
  protected readonly loggedIn = this.auth.loggedIn;
  protected readonly displayName = displayName;

  /** Neue Lieferadresse (0017); gleiche Regeln wie im Backend. */
  protected readonly address = createAddressForm();
  /** Im Konto gespeicherte Adressen (E2) und welche gewählt ist; 'neu' = Formular für eine neue Adresse. */
  protected readonly savedAddresses = signal<SavedAddress[]>([]);
  protected readonly selectedAddress = signal<number | 'neu'>('neu');
  protected readonly saveNewAddress = new FormControl(true, { nonNullable: true });
  protected readonly addressLine = addressLine;
  /** Wurde die Adresse aus der letzten Bestellung übernommen? Dann zeigen wir einen Hinweis. */
  protected readonly addressFromLastOrder = signal(false);

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

  // ---- Rabattcode (G2) ----
  protected readonly codeInput = new FormControl('', { nonNullable: true });
  /** Eingelöster und geprüfter Code; verbindlich rechnet das Backend beim Bestellen. */
  protected readonly discount = signal<DiscountCheck | null>(null);
  protected readonly codeError = signal<string | null>(null);
  protected readonly checkingCode = signal(false);
  /** Nur zur Anzeige, gerundet wie im Backend (kaufmännisch auf Cent). */
  protected readonly discountAmount = computed(() => {
    const discount = this.discount();
    return discount ? Math.round(this.total() * discount.percent) / 100 : 0;
  });
  protected readonly totalAfterDiscount = computed(() => this.total() - this.discountAmount());

  constructor() {
    this.reload();
    // Sobald klar ist, dass jemand angemeldet ist: Adresse vorbelegen.
    effect(() => {
      if (this.loggedIn()) {
        untracked(() => this.loadAddresses());
      }
    });
  }

  /** Aktuelle Preise und Bestände für alle Produkte im Warenkorb laden. */
  protected reload(): void {
    const ids = this.cart.items().map((item) => item.productId);
    this.loading.set(true);
    this.loadError.set(null);
    if (ids.length === 0) {
      this.loaded.set(new Map());
      this.loading.set(false);
      return;
    }
    forkJoin(ids.map((id) => this.loadProduct(id))).subscribe({
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

  protected applyCode(): void {
    const code = this.codeInput.value.trim();
    if (!code || this.checkingCode()) {
      return;
    }
    if (!this.loggedIn()) {
      this.codeError.set('Bitte melde dich an, um einen Rabattcode einzulösen.');
      return;
    }
    this.checkingCode.set(true);
    this.codeError.set(null);
    this.discounts.check(code).subscribe({
      next: (result) => {
        this.checkingCode.set(false);
        this.discount.set(result);
        this.codeInput.setValue('');
      },
      error: (error) => {
        this.checkingCode.set(false);
        this.discount.set(null);
        this.codeError.set(userMessage(error));
      },
    });
  }

  protected removeCode(): void {
    this.discount.set(null);
    this.codeError.set(null);
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
    const useNew = this.selectedAddress() === 'neu';
    if (useNew && this.address.invalid) {
      this.address.markAllAsTouched();
      this.orderError.set('Bitte gib eine vollständige Lieferadresse an.');
      return;
    }
    this.ordering.set(true);
    this.orderError.set(null);
    this.chosenAddress().subscribe({
      next: (choice) => this.place(choice),
      error: (error) => this.showOrderError(error),
    });
  }

  /**
   * Welche Adresse geht mit? Gespeicherte Adresse → nur ihre ID. Neue Adresse → entweder vorher im
   * Konto speichern (dann ID) oder direkt mitschicken.
   */
  private chosenAddress(): Observable<AddressChoice> {
    const selected = this.selectedAddress();
    if (selected !== 'neu') {
      return of({ addressId: selected });
    }
    const value = addressValue(this.address);
    if (this.saveNewAddress.value && this.savedAddresses().length < 10) {
      return this.account
        .addAddress({ ...value, label: null })
        .pipe(map((saved) => ({ addressId: saved.id })));
    }
    return of({ deliveryAddress: value });
  }

  private place(choice: AddressChoice): void {
    this.orders.place(this.cart.items(), choice, this.discount()?.code).subscribe({
      next: (order) => {
        this.cart.clear();
        this.notify.success('Danke! Deine Bestellung ist eingegangen.');
        this.router.navigate(['/bestellungen', order.id]);
      },
      error: (error) => this.showOrderError(error),
    });
  }

  private showOrderError(error: unknown): void {
    this.ordering.set(false);
    if (error instanceof HttpErrorResponse && error.status === 401) {
      this.router.navigate(['/anmelden'], { queryParams: { zurueck: '/warenkorb' } });
      return;
    }
    const errors = fieldErrors(error);
    const codeProblem = errors.find((e) => e.field === 'discountCode');
    if (codeProblem) {
      this.discount.set(null);
      this.codeError.set(codeProblem.message);
      this.orderError.set('Der Rabattcode gilt nicht mehr. Du kannst ohne ihn bestellen.');
      return;
    }
    // Feldfehler zur Adresse: beim Bestellen "deliveryAddress.city", beim Speichern im Konto "city".
    const addressErrors = errors.filter(
      (e) => e.field !== 'items' && e.field !== 'addressId' && e.field !== 'discountCode',
    );
    if (
      applyAddressErrors(
        this.address,
        addressErrors,
        addressErrors[0]?.field.startsWith('deliveryAddress') ? 'deliveryAddress' : '',
      )
    ) {
      this.selectedAddress.set('neu');
      this.orderError.set('Bitte prüfe die Lieferadresse.');
      return;
    }
    if (errors.some((e) => e.field === 'addressId')) {
      this.orderError.set('Diese Adresse gibt es nicht mehr. Bitte wähle eine andere.');
      this.loadAddresses();
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
  }

  /**
   * Gespeicherte Adressen laden (E2) und die Standardadresse vorauswählen. Gibt es keine,
   * wird das Formular mit der Adresse der letzten Bestellung vorausgefüllt (0017).
   * Schlägt das Laden fehl, bleibt einfach das leere Formular, Bestellen geht trotzdem.
   */
  protected loadAddresses(): void {
    this.account.addresses().subscribe({
      next: (list) => {
        this.savedAddresses.set(list);
        const standard = list.find((address) => address.standard) ?? list[0];
        if (standard) {
          this.selectedAddress.set(standard.id);
        } else {
          this.selectedAddress.set('neu');
          this.prefillFromLastOrder();
        }
      },
      error: () => this.prefillFromLastOrder(),
    });
  }

  private prefillFromLastOrder(): void {
    if (this.address.dirty) {
      return;
    }
    const user = this.auth.user();
    if (user && !this.address.controls.name.value) {
      this.address.controls.name.setValue(user.name);
    }
    this.orders.list({ page: 0 }).subscribe({
      next: (page) => {
        const last = page.items.find((order) => order.deliveryAddress)?.deliveryAddress;
        if (last && !this.address.dirty) {
          this.address.setValue({ ...last });
          this.addressFromLastOrder.set(true);
        }
      },
      error: () => undefined,
    });
  }

  /** 404 = Produkt wurde gelöscht → null; andere Fehler brechen das Laden ab. */
  private loadProduct(id: number): Observable<Product | null> {
    return this.products
      .get(id)
      .pipe(
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

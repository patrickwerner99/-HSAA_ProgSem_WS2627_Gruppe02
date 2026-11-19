import { registerLocaleData } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import localeDe from '@angular/common/locales/de';
import { DEFAULT_CURRENCY_CODE, LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AuthService } from '../../api/auth.service';
import { Product } from '../../api/models';
import { CartService } from '../../shared/cart.service';
import { CartPage, problemFor } from './cart';

registerLocaleData(localeDe);

const product = (id: number, overrides: Partial<Product> = {}): Product => ({
  id,
  name: `Produkt ${id}`,
  description: null,
  price: 24.9,
  stock: 10,
  stockStatus: 'IN_STOCK',
  imageUrl: null,
  categoryId: 1,
  categoryName: 'Kreatin',
  size: '500 g',
  form: 'PULVER',
  regularPrice: 24.9,
  onSale: false,
  savingPercent: 0,
  salePrice: null,
  saleFrom: null,
  saleUntil: null,
  returnedGoods: false,
  ean: null,
  ...overrides,
});

describe('problemFor', () => {
  it('meldet zu wenig Bestand mit genauer Zahl', () => {
    expect(problemFor(product(1, { stock: 2 }), 5)).toBe('Nur noch 2 verfügbar.');
    expect(problemFor(product(1, { stock: 0 }), 1)).toBe('Ausverkauft.');
    expect(problemFor(null, 1)).toBe('Dieses Produkt gibt es nicht mehr.');
    expect(problemFor(product(1), 3)).toBeNull();
  });
});

describe('CartPage', () => {
  let fixture: ComponentFixture<CartPage>;
  let http: HttpTestingController;
  let cart: CartService;

  async function setUp(loggedIn: boolean) {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [CartPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: LOCALE_ID, useValue: 'de-DE' },
        { provide: DEFAULT_CURRENCY_CODE, useValue: 'EUR' },
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    cart = TestBed.inject(CartService);
    cart.add(1, 2);

    const auth = TestBed.inject(AuthService);
    auth.load().subscribe();
    http
      .expectOne('/api/auth/me')
      .flush(
        loggedIn ? { id: 5, email: 'lena@example.de', name: 'Lena', role: 'CUSTOMER' } : null,
        loggedIn ? {} : { status: 401, statusText: 'Unauthorized' },
      );

    fixture = TestBed.createComponent(CartPage);
    http.expectOne('/api/products/1').flush(product(1));
    await fixture.whenStable();
    if (loggedIn) {
      // Gespeicherte Adressen (E2); ohne welche: Vorbelegung aus der letzten Bestellung (0017)
      http.expectOne('/api/account/addresses').flush(savedAddresses);
      if (savedAddresses.length === 0) {
        http
          .expectOne((request) => request.url === '/api/orders')
          .flush({
            items: lastOrders,
            page: 0,
            size: 10,
            totalItems: lastOrders.length,
            last: true,
          });
      }
      await fixture.whenStable();
    }
  }

  let lastOrders: unknown[] = [];
  let savedAddresses: unknown[] = [];
  beforeEach(() => {
    lastOrders = [];
    savedAddresses = [];
  });

  const ADDRESS = {
    name: 'Lena Muster',
    street: 'Beethovenstraße 1',
    postalCode: '73430',
    city: 'Aalen',
  };

  /** Neue Adresse eingeben, ohne sie im Konto zu speichern. */
  function fillAddress() {
    fixture.componentInstance['address'].setValue({ ...ADDRESS });
    fixture.componentInstance['saveNewAddress'].setValue(false);
  }

  const text = () => (fixture.nativeElement as HTMLElement).textContent ?? '';
  const orderButton = () =>
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button.order')!;

  it('zeigt Preise vom Shop und die Summe', async () => {
    await setUp(true);
    expect(text()).toContain('Produkt 1 (500 g)');
    expect(text()).toMatch(/49,80\s€/);
  });

  it('schickt Gäste beim Bestellen zur Anmeldung, der Warenkorb bleibt', async () => {
    await setUp(false);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    expect(orderButton().textContent).toContain('Anmelden und bestellen');

    orderButton().click();

    expect(navigate).toHaveBeenCalledWith(['/anmelden'], {
      queryParams: { zurueck: '/warenkorb' },
    });
    http.expectNone('/api/orders');
    expect(cart.count()).toBe(2);
  });

  it('schickt beim Bestellen nur ID, Menge und Lieferadresse, keinen Preis', async () => {
    await setUp(true);
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fillAddress();
    orderButton().click();

    const request = http.expectOne('/api/orders');
    expect(request.request.body).toEqual({
      items: [{ productId: 1, quantity: 2 }],
      deliveryAddress: ADDRESS,
    });
    request.flush({ id: 42 }, { status: 201, statusText: 'Created' });
    expect(cart.count()).toBe(0);
  });

  it('zeigt bei 409 den neuen Bestand an der Position', async () => {
    await setUp(true);
    fillAddress();
    orderButton().click();
    http.expectOne('/api/orders').flush(
      {
        status: 409,
        detail: 'Nicht genug auf Lager',
        items: [{ productId: 1, productName: 'Produkt 1', requested: 2, available: 1 }],
      },
      { status: 409, statusText: 'Conflict' },
    );
    http.expectOne('/api/products/1').flush(product(1, { stock: 1, stockStatus: 'LOW_STOCK' }));
    await fixture.whenStable();

    expect(text()).toContain('Inzwischen hat sich der Bestand geändert');
    expect(text()).toContain('Nur noch 1 verfügbar.');
    expect(orderButton().disabled).toBe(true);
  });

  it('bestellt ohne vollständige Lieferadresse nicht', async () => {
    await setUp(true);
    orderButton().click();
    await fixture.whenStable();

    http.expectNone('/api/orders');
    expect(text()).toContain('Bitte gib eine vollständige Lieferadresse an.');
  });

  it('übernimmt die Adresse der letzten Bestellung', async () => {
    lastOrders = [{ id: 7, deliveryAddress: ADDRESS }];
    await setUp(true);

    expect(fixture.componentInstance['address'].getRawValue()).toEqual(ADDRESS);
    expect(text()).toContain('Aus deiner letzten Bestellung übernommen');
  });

  it('zeigt Feldfehler des Backends an der Adresse', async () => {
    await setUp(true);
    fillAddress();
    orderButton().click();
    http.expectOne('/api/orders').flush(
      {
        status: 400,
        detail: 'Bitte prüfe die markierten Felder.',
        errors: [
          { field: 'deliveryAddress.postalCode', message: 'Die Postleitzahl hat genau 5 Ziffern.' },
        ],
      },
      { status: 400, statusText: 'Bad Request' },
    );
    await fixture.whenStable();

    expect(text()).toContain('Bitte prüfe die Lieferadresse.');
    expect(fixture.componentInstance['address'].controls.postalCode.errors).toEqual({
      server: 'Die Postleitzahl hat genau 5 Ziffern.',
    });
  });

  it('wählt die Standardadresse aus dem Konto und schickt nur ihre ID', async () => {
    savedAddresses = [
      { id: 3, label: 'Arbeit', standard: false, ...ADDRESS },
      { id: 4, label: 'Zuhause', standard: true, ...ADDRESS },
    ];
    await setUp(true);
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    expect(text()).toContain('Zuhause');

    orderButton().click();
    const request = http.expectOne('/api/orders');
    expect(request.request.body).toEqual({ items: [{ productId: 1, quantity: 2 }], addressId: 4 });
  });

  it('speichert eine neue Adresse auf Wunsch zuerst im Konto', async () => {
    await setUp(true);
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture.componentInstance['address'].setValue({ ...ADDRESS });

    orderButton().click();
    const save = http.expectOne('/api/account/addresses');
    expect(save.request.method).toBe('POST');
    save.flush({ id: 9, label: null, standard: true, ...ADDRESS });
    const request = http.expectOne('/api/orders');
    expect(request.request.body).toEqual({ items: [{ productId: 1, quantity: 2 }], addressId: 9 });
  });

  it('löst einen Rabattcode ein, zeigt den Rabatt und schickt den Code mit', async () => {
    await setUp(true);
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture.componentInstance['codeInput'].setValue('hyrox10');
    fixture.componentInstance['applyCode']();
    http
      .expectOne('/api/discount-codes/hyrox10')
      .flush({ code: 'HYROX10', percent: 10, validUntil: '2026-12-31' });
    await fixture.whenStable();

    expect(text()).toContain('HYROX10');
    expect(text()).toMatch(/44,82\s€/);

    fillAddress();
    orderButton().click();
    const request = http.expectOne('/api/orders');
    expect(request.request.body.discountCode).toBe('HYROX10');
  });

  it('zeigt, warum ein Rabattcode nicht gilt', async () => {
    await setUp(true);
    fixture.componentInstance['codeInput'].setValue('SOMMER20');
    fixture.componentInstance['applyCode']();
    http
      .expectOne('/api/discount-codes/SOMMER20')
      .flush(
        { status: 409, detail: 'Dieser Rabattcode ist abgelaufen.' },
        { status: 409, statusText: 'Conflict' },
      );
    await fixture.whenStable();

    expect(text()).toContain('Dieser Rabattcode ist abgelaufen.');
    expect(text()).toMatch(/49,80\s€/);
  });
});

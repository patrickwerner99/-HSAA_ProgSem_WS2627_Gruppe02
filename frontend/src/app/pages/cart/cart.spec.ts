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
  }

  const text = () => (fixture.nativeElement as HTMLElement).textContent ?? '';
  const orderButton = () =>
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('button.order')!;

  it('zeigt Preise vom Shop und die Summe', async () => {
    await setUp(true);
    expect(text()).toContain('Produkt 1');
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

  it('schickt beim Bestellen nur ID und Menge, keinen Preis', async () => {
    await setUp(true);
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    orderButton().click();

    const request = http.expectOne('/api/orders');
    expect(request.request.body).toEqual({ items: [{ productId: 1, quantity: 2 }] });
    request.flush({ id: 42 }, { status: 201, statusText: 'Created' });
    expect(cart.count()).toBe(0);
  });

  it('zeigt bei 409 den neuen Bestand an der Position', async () => {
    await setUp(true);
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
});

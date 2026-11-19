import { registerLocaleData } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import localeDe from '@angular/common/locales/de';
import { DEFAULT_CURRENCY_CODE, LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Product } from '../../api/models';
import { ProductDetailPage } from './product-detail';

registerLocaleData(localeDe);

const whey: Product = {
  id: 7,
  name: 'Whey Protein Schoko',
  description: null,
  price: 34.9,
  stock: 10,
  stockStatus: 'IN_STOCK',
  imageUrl: null,
  categoryId: 1,
  categoryName: 'Protein',
  size: '1 kg',
  form: 'PULVER',
  regularPrice: 34.9,
  onSale: false,
  savingPercent: 0,
  salePrice: null,
  saleFrom: null,
  saleUntil: null,
  returnedGoods: false,
  ean: '4058172511097',
};

/** Sprint 3, H1/H2: Nährwerte kommen nach dem Produkt; fehlen sie, bleibt die Seite nutzbar. */
describe('ProductDetailPage – Nährwerte', () => {
  let fixture: ComponentFixture<ProductDetailPage>;
  let httpMock: HttpTestingController;

  async function open(product: Product): Promise<void> {
    fixture = TestBed.createComponent(ProductDetailPage);
    fixture.componentRef.setInput('id', String(product.id));
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    httpMock.expectOne(`/api/products/${product.id}`).flush(product);
    httpMock
      .expectOne((r) => r.url === '/api/products')
      .flush({ items: [], page: 0, size: 50, totalItems: 0, last: true });
    await fixture.whenStable();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductDetailPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: LOCALE_ID, useValue: 'de-DE' },
        { provide: DEFAULT_CURRENCY_CODE, useValue: 'EUR' },
      ],
    }).compileComponents();
  });

  afterEach(() => httpMock.verify());

  const text = () => (fixture.nativeElement as HTMLElement).textContent ?? '';

  it('zeigt Nährwerte mit Quelle', async () => {
    await open(whey);
    httpMock.expectOne('/api/products/7/nutrition').flush({
      ean: '4058172511097',
      productName: 'WHEY Protein Schoko-Geschmack',
      brands: 'Sportness',
      energyKcal: 378,
      protein: 78,
      carbohydrates: 5.4,
      sugars: 3.2,
      fat: 4.9,
      saturatedFat: 2.1,
      salt: 0.6,
      nutriScore: null,
      sourceUrl: 'https://world.openfoodfacts.org/product/4058172511097',
      source: 'Open Food Facts',
      fetchedAt: '2026-11-19T18:00:00Z',
      stale: false,
    });
    await fixture.whenStable();

    expect(text()).toContain('Nährwerte pro 100 g');
    expect(text()).toContain('78 g');
    expect(text()).toContain('5,4 g');
    expect(text()).toContain('Open Food Facts');
    expect(text()).toContain('Sportness');
  });

  it('API nicht erreichbar (503): Hinweis, Produkt bleibt kaufbar', async () => {
    await open(whey);
    httpMock
      .expectOne('/api/products/7/nutrition')
      .flush(
        { title: 'Gerade nicht verfügbar' },
        { status: 503, statusText: 'Service Unavailable' },
      );
    await fixture.whenStable();

    expect(text()).toContain('Nährwerte sind gerade nicht verfügbar');
    expect(text()).toContain('In den Warenkorb');
  });

  it('ohne Barcode keine Anfrage und kein Abschnitt', async () => {
    await open({ ...whey, ean: null });

    httpMock.expectNone('/api/products/7/nutrition');
    expect(text()).not.toContain('Nährwerte');
  });
});

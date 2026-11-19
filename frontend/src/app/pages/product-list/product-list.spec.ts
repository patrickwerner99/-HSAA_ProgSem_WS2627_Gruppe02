import { registerLocaleData } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import localeDe from '@angular/common/locales/de';
import { DEFAULT_CURRENCY_CODE, LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Product } from '../../api/models';
import { ProductListPage } from './product-list';

registerLocaleData(localeDe);

const product = (id: number, name: string, overrides: Partial<Product> = {}): Product => ({
  id,
  name,
  description: null,
  price: 34.9,
  stock: 10,
  stockStatus: 'IN_STOCK',
  imageUrl: null,
  categoryId: 1,
  size: '500 g',
  form: 'PULVER',
  regularPrice: 34.9,
  onSale: false,
  savingPercent: 0,
  salePrice: null,
  saleFrom: null,
  saleUntil: null,
  returnedGoods: false,
  ean: null,
  categoryName: 'Protein',
  ...overrides,
});

describe('ProductListPage', () => {
  let fixture: ComponentFixture<ProductListPage>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductListPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: LOCALE_ID, useValue: 'de-DE' },
        { provide: DEFAULT_CURRENCY_CODE, useValue: 'EUR' },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ProductListPage);
    httpMock = TestBed.inject(HttpTestingController);
    httpMock.expectOne('/api/categories').flush([{ id: 1, name: 'Protein', productCount: 2 }]);
  });

  const text = () => (fixture.nativeElement as HTMLElement).textContent ?? '';
  const productsRequest = () => httpMock.expectOne((r) => r.url === '/api/products');

  it('zeigt Name und Preis im deutschen Format', async () => {
    productsRequest().flush({
      items: [
        product(1, 'Whey Protein Schoko'),
        product(2, 'Whey Protein Erdbeere', { stock: 0, stockStatus: 'SOLD_OUT' }),
      ],
      page: 0,
      size: 12,
      totalItems: 2,
      last: true,
    });
    await fixture.whenStable();

    expect(text()).toContain('Whey Protein Schoko');
    expect(text()).toMatch(/34,90\s€/);
    expect(text()).toContain('Ausverkauft');
    expect(text()).toContain('Alle Produkte angezeigt');
  });

  it('zeigt bei fehlendem Bild den Platzhalter', async () => {
    productsRequest().flush({
      items: [product(1, 'Kreatin')],
      page: 0,
      size: 12,
      totalItems: 1,
      last: true,
    });
    await fixture.whenStable();

    const img = (fixture.nativeElement as HTMLElement).querySelector('img');
    expect(img?.getAttribute('src')).toBe('/images/placeholder.svg');
  });

  it('zeigt eine Meldung mit "Erneut versuchen", wenn das Backend nicht erreichbar ist', async () => {
    productsRequest().flush(null, { status: 0, statusText: 'Unknown Error' });
    await fixture.whenStable();

    expect(text()).toContain('Die Produkte konnten nicht geladen werden.');
    expect(text()).toContain('Erneut versuchen');
  });

  it('meldet "Noch keine Produkte vorhanden" bei leerem Shop', async () => {
    productsRequest().flush({ items: [], page: 0, size: 12, totalItems: 0, last: true });
    await fixture.whenStable();

    expect(text()).toContain('Noch keine Produkte vorhanden.');
  });

  it('übernimmt Sortierung und "Nur verfügbare" aus der URL (C2)', async () => {
    productsRequest().flush({ items: [], page: 0, size: 12, totalItems: 0, last: true });
    await fixture.whenStable();

    await TestBed.inject(Router).navigateByUrl('/?sort=price-desc&verfuegbar=ja');
    const request = productsRequest();
    expect(request.request.params.get('sort')).toBe('price-desc');
    expect(request.request.params.get('inStock')).toBe('true');
    request.flush({ items: [], page: 0, size: 12, totalItems: 0, last: true });
  });

  it('ignoriert eine unbekannte Sortierung in der URL', async () => {
    productsRequest().flush({ items: [], page: 0, size: 12, totalItems: 0, last: true });
    await fixture.whenStable();

    await TestBed.inject(Router).navigateByUrl('/?sort=billig');
    expect(productsRequest().request.params.has('sort')).toBe(false);
  });

  it('zeigt bei wenig Bestand die genaue Zahl', async () => {
    productsRequest().flush({
      items: [product(1, 'EAA Eistee', { stock: 3, stockStatus: 'LOW_STOCK' })],
      page: 0,
      size: 12,
      totalItems: 1,
      last: true,
    });
    await fixture.whenStable();
    expect(text()).toContain('Nur noch 3');
  });
});

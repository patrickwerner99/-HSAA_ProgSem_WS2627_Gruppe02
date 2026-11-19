import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { PAGE_SIZE, ProductService } from './product.service';

describe('ProductService', () => {
  let service: ProductService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ProductService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('fragt die erste Ladung relativ zur eigenen Adresse ab', () => {
    service.search({}).subscribe();
    const request = httpMock.expectOne((r) => r.url === '/api/products');
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('size')).toBe(String(PAGE_SIZE));
    expect(request.request.params.has('q')).toBe(false);
    request.flush({ items: [], page: 0, size: PAGE_SIZE, totalItems: 0, last: true });
  });

  it('schickt Suche und Kategorie mit', () => {
    service.search({ q: 'whey', categoryId: 2, page: 1 }).subscribe();
    const request = httpMock.expectOne((r) => r.url === '/api/products');
    expect(request.request.params.get('q')).toBe('whey');
    expect(request.request.params.get('categoryId')).toBe('2');
    expect(request.request.params.get('page')).toBe('1');
    request.flush({ items: [], page: 1, size: PAGE_SIZE, totalItems: 0, last: true });
  });

  it('ändert einzelne Felder per PATCH', () => {
    service.patch(7, { stock: 10 }).subscribe();
    const request = httpMock.expectOne('/api/products/7');
    expect(request.request.method).toBe('PATCH');
    expect(request.request.body).toEqual({ stock: 10 });
    request.flush({});
  });

  it('schickt die Produktart als Parameter form (0019)', () => {
    service.search({ form: 'RIEGEL' }).subscribe();
    const request = httpMock.expectOne((r) => r.url === '/api/products');
    expect(request.request.params.get('form')).toBe('RIEGEL');
    request.flush({ items: [], page: 0, size: PAGE_SIZE, totalItems: 0, last: true });
  });
});

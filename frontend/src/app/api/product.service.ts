import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse, Product, ProductInput } from './models';

/** Eine Ladung beim endlosen Scrollen (ADR-0002), passend zum Backend. */
export const PAGE_SIZE = 12;

export interface ProductQuery {
  q?: string | null;
  categoryId?: number | null;
  page?: number;
  size?: number;
}

/**
 * Alle HTTP-Aufrufe für Produkte. Relative URLs: nginx leitet /api an das Backend weiter,
 * deshalb gibt es kein CORS und das Frontend muss nicht wissen, wo das Backend läuft.
 */
@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);

  search(query: ProductQuery): Observable<PageResponse<Product>> {
    let params = new HttpParams().set('page', query.page ?? 0).set('size', query.size ?? PAGE_SIZE);
    if (query.q) {
      params = params.set('q', query.q);
    }
    if (query.categoryId != null) {
      params = params.set('categoryId', query.categoryId);
    }
    return this.http.get<PageResponse<Product>>('/api/products', { params });
  }

  get(id: number): Observable<Product> {
    return this.http.get<Product>(`/api/products/${id}`);
  }

  create(input: ProductInput): Observable<Product> {
    return this.http.post<Product>('/api/products', input);
  }

  /** Ganzes Produkt ändern. */
  replace(id: number, input: ProductInput): Observable<Product> {
    return this.http.put<Product>(`/api/products/${id}`, input);
  }

  /** Einzelne Felder ändern, z. B. nur den Bestand. */
  patch(id: number, changes: Partial<ProductInput>): Observable<Product> {
    return this.http.patch<Product>(`/api/products/${id}`, changes);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/products/${id}`);
  }
}

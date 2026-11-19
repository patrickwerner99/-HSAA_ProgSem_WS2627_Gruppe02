import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse, ProductReturn, ReturnInput, ReturnStatus, ReturnUpdate } from './models';

/** Retouren (Sprint 3, F1/F2): Kunden sehen ihre eigenen, Admins alle (filtert das Backend). */
@Injectable({ providedIn: 'root' })
export class ReturnService {
  private readonly http = inject(HttpClient);

  create(input: ReturnInput): Observable<ProductReturn> {
    return this.http.post<ProductReturn>('/api/returns', input);
  }

  list(query: {
    status?: ReturnStatus | null;
    page?: number;
  }): Observable<PageResponse<ProductReturn>> {
    let params = new HttpParams().set('page', query.page ?? 0).set('size', 20);
    if (query.status) {
      params = params.set('status', query.status);
    }
    return this.http.get<PageResponse<ProductReturn>>('/api/returns', { params });
  }

  forOrder(orderId: number): Observable<ProductReturn[]> {
    return this.http.get<ProductReturn[]>(`/api/orders/${orderId}/returns`);
  }

  update(id: number, update: ReturnUpdate): Observable<ProductReturn> {
    return this.http.patch<ProductReturn>(`/api/returns/${id}`, update);
  }
}

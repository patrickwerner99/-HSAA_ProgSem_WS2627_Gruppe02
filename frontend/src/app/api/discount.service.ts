import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { DiscountCheck, DiscountCode, DiscountCodeInput } from './models';

/** Rabattcodes (Sprint 3, G2): pflegen nur der Admin, prüfen jeder Angemeldete. */
@Injectable({ providedIn: 'root' })
export class DiscountService {
  private readonly http = inject(HttpClient);

  list(): Observable<DiscountCode[]> {
    return this.http.get<DiscountCode[]>('/api/discount-codes');
  }

  create(input: DiscountCodeInput): Observable<DiscountCode> {
    return this.http.post<DiscountCode>('/api/discount-codes', input);
  }

  setActive(id: number, active: boolean): Observable<DiscountCode> {
    return this.http.patch<DiscountCode>(`/api/discount-codes/${id}`, { active });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/discount-codes/${id}`);
  }

  /** Gilt der Code heute? 404 = gibt es nicht, 409 = abgelaufen/abgeschaltet/aufgebraucht. */
  check(code: string): Observable<DiscountCheck> {
    return this.http.get<DiscountCheck>(`/api/discount-codes/${encodeURIComponent(code.trim())}`);
  }
}

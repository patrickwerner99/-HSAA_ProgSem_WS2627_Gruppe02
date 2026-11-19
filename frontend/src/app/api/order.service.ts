import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { DeliveryAddress, Order, OrderItem, OrderStatus, PageResponse } from './models';

export const ORDER_PAGE_SIZE = 10;

export type AddressChoice = { addressId: number } | { deliveryAddress: DeliveryAddress };

/** Bestellungen: Kunden sehen ihre eigenen, Admins alle (filtert das Backend). */
@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);

  /**
   * Bestellen mit Lieferadresse: entweder eine gespeicherte Adresse aus dem Konto (addressId, E2)
   * oder eine neu eingegebene (deliveryAddress, 0017). Preise schickt der Browser nie mit (B3).
   */
  place(
    items: OrderItem[],
    address: AddressChoice,
    discountCode?: string | null,
  ): Observable<Order> {
    return this.http.post<Order>('/api/orders', {
      items,
      ...address,
      ...(discountCode ? { discountCode } : {}),
    });
  }

  list(query: { status?: OrderStatus | null; page?: number }): Observable<PageResponse<Order>> {
    let params = new HttpParams().set('page', query.page ?? 0).set('size', ORDER_PAGE_SIZE);
    if (query.status) {
      params = params.set('status', query.status);
    }
    return this.http.get<PageResponse<Order>>('/api/orders', { params });
  }

  get(id: number): Observable<Order> {
    return this.http.get<Order>(`/api/orders/${id}`);
  }

  changeStatus(id: number, status: OrderStatus): Observable<Order> {
    return this.http.patch<Order>(`/api/orders/${id}`, { status });
  }
}

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { SavedAddress, SavedAddressInput, User } from './models';

export interface AccountUpdate {
  name?: string;
  email?: string;
  /** nur nötig, wenn die E-Mail geändert wird */
  currentPassword?: string;
}

/** Das eigene Konto unter /api/account (Sprint 3, E1 und E2). */
@Injectable({ providedIn: 'root' })
export class AccountService {
  private readonly http = inject(HttpClient);

  update(changes: AccountUpdate): Observable<User> {
    return this.http.patch<User>('/api/account', changes);
  }

  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.http.put<void>('/api/account/password', { currentPassword, newPassword });
  }

  addresses(): Observable<SavedAddress[]> {
    return this.http.get<SavedAddress[]>('/api/account/addresses');
  }

  addAddress(input: SavedAddressInput): Observable<SavedAddress> {
    return this.http.post<SavedAddress>('/api/account/addresses', input);
  }

  updateAddress(id: number, input: SavedAddressInput): Observable<SavedAddress> {
    return this.http.put<SavedAddress>(`/api/account/addresses/${id}`, input);
  }

  deleteAddress(id: number): Observable<void> {
    return this.http.delete<void>(`/api/account/addresses/${id}`);
  }
}

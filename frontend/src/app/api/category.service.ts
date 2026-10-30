import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Category } from './models';

/** Alle HTTP-Aufrufe für Kategorien. */
@Injectable({ providedIn: 'root' })
export class CategoryService {
  private readonly http = inject(HttpClient);

  list(): Observable<Category[]> {
    return this.http.get<Category[]>('/api/categories');
  }

  create(name: string): Observable<Category> {
    return this.http.post<Category>('/api/categories', { name });
  }

  rename(id: number, name: string): Observable<Category> {
    return this.http.put<Category>(`/api/categories/${id}`, { name });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/categories/${id}`);
  }
}

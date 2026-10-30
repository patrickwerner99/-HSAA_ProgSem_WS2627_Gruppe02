import { Injectable, computed, signal } from '@angular/core';
import { OrderItem } from '../api/models';

/** Mehr als 99 Stück eines Produkts nimmt auch das Backend nicht an. */
export const MAX_QUANTITY = 99;

const STORAGE_KEY = 'hyrox-shop-warenkorb';

/**
 * Warenkorb im Browser (ADR-0005). Gespeichert werden nur Produkt-ID und Menge, keine Preise
 * und kein Bestand: Beides holt die Warenkorbseite immer frisch vom Shop, und beim Bestellen
 * rechnet allein das Backend (B3). So kann man ohne Konto einkaufen und meldet sich erst
 * beim Bestellen an.
 */
@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly state = signal<OrderItem[]>(readStorage());

  readonly items = this.state.asReadonly();
  /** Anzahl Artikel für das Symbol in der Kopfzeile. */
  readonly count = computed(() => this.state().reduce((sum, item) => sum + item.quantity, 0));

  constructor() {
    // Warenkorb in einem anderen Tab geändert → hier übernehmen.
    if (typeof window !== 'undefined') {
      window.addEventListener('storage', (event) => {
        if (event.key === STORAGE_KEY) {
          this.state.set(readStorage());
        }
      });
    }
  }

  quantityOf(productId: number): number {
    return this.state().find((item) => item.productId === productId)?.quantity ?? 0;
  }

  /** Legt Menge dazu; nie mehr als MAX_QUANTITY. Liefert die neue Menge. */
  add(productId: number, quantity: number): number {
    const next = clamp(this.quantityOf(productId) + quantity);
    this.set(productId, next);
    return next;
  }

  /** Menge setzen; 0 entfernt das Produkt. */
  set(productId: number, quantity: number): void {
    const value = clamp(quantity);
    const rest = this.state().filter((item) => item.productId !== productId);
    const exists = rest.length !== this.state().length;
    let next: OrderItem[];
    if (value <= 0) {
      next = rest;
    } else if (exists) {
      next = this.state().map((item) =>
        item.productId === productId ? { productId, quantity: value } : item,
      );
    } else {
      next = [...this.state(), { productId, quantity: value }];
    }
    this.save(next);
  }

  remove(productId: number): void {
    this.set(productId, 0);
  }

  clear(): void {
    this.save([]);
  }

  private save(items: OrderItem[]): void {
    this.state.set(items);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
    } catch {
      // Privates Fenster oder Speicher voll: Warenkorb gilt dann nur bis zum Neuladen.
    }
  }
}

function clamp(quantity: number): number {
  if (!Number.isFinite(quantity)) {
    return 0;
  }
  return Math.max(0, Math.min(MAX_QUANTITY, Math.floor(quantity)));
}

/** Liest den gespeicherten Warenkorb. Kaputte oder manipulierte Einträge werden verworfen. */
export function readStorage(): OrderItem[] {
  try {
    const raw = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '[]');
    if (!Array.isArray(raw)) {
      return [];
    }
    const seen = new Set<number>();
    const items: OrderItem[] = [];
    for (const entry of raw) {
      const productId = Number(entry?.productId);
      const quantity = clamp(Number(entry?.quantity));
      if (Number.isInteger(productId) && productId > 0 && quantity > 0 && !seen.has(productId)) {
        seen.add(productId);
        items.push({ productId, quantity });
      }
    }
    return items;
  } catch {
    return [];
  }
}

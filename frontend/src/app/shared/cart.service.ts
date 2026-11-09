import { Injectable, computed, signal } from '@angular/core';
import { OrderItem } from '../api/models';

/** Mehr als 99 Stück eines Produkts nimmt auch das Backend nicht an. */
export const MAX_QUANTITY = 99;

const STORAGE_KEY = 'hyrox-shop-warenkorb';

/**
 * Warenkorb im Browser (ADR-0005). Gespeichert werden nur Produkt-ID und Menge, keine Preise:
 * Die holt die Warenkorbseite immer frisch vom Shop, und beim Bestellen rechnet allein das
 * Backend (B3).
 */
@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly state = signal<OrderItem[]>(readStorage());

  readonly items = this.state.asReadonly();
  /** Anzahl Artikel für das Symbol in der Kopfzeile. */
  readonly count = computed(() => this.state().reduce((sum, item) => sum + item.quantity, 0));

  quantityOf(productId: number): number {
    return this.state().find((item) => item.productId === productId)?.quantity ?? 0;
  }

  /** Menge dazulegen. */
  add(productId: number, quantity: number): void {
    this.set(productId, this.quantityOf(productId) + quantity);
  }

  /** Menge setzen (höchstens 99); 0 entfernt das Produkt. */
  set(productId: number, quantity: number): void {
    const value = Math.min(quantity, MAX_QUANTITY);
    const items = this.state().filter((item) => item.productId !== productId);
    if (value > 0) {
      // An der alten Stelle bleiben, damit die Liste beim Ändern der Menge nicht springt.
      const index = this.state().findIndex((item) => item.productId === productId);
      items.splice(index === -1 ? items.length : index, 0, { productId, quantity: value });
    }
    this.save(items);
  }

  remove(productId: number): void {
    this.set(productId, 0);
  }

  clear(): void {
    this.save([]);
  }

  private save(items: OrderItem[]): void {
    this.state.set(items);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
  }
}

/** Gespeicherten Warenkorb lesen. Ist der Inhalt kaputt, gibt es einen leeren Warenkorb. */
function readStorage(): OrderItem[] {
  try {
    const items = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '[]');
    return Array.isArray(items) ? items : [];
  } catch {
    return [];
  }
}

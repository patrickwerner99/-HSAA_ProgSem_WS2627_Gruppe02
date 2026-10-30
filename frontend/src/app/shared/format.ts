import { StockStatus } from '../api/models';

/** Platzhalter, wenn ein Produkt kein Bild hat oder das Bild nicht lädt. */
export const PLACEHOLDER_IMAGE = '/images/placeholder.svg';

/** Gleiche Regel wie im Backend (ProductRules.IMAGE_URL_PATTERN). */
export const IMAGE_URL_PATTERN =
  /^(https?:\/\/\S+|\/images\/[A-Za-z0-9._-]+|\/api\/images\/[A-Za-z0-9._-]+)?$/;

/** Erlaubt "34", "34,9", "34,90" und "34.90", höchstens 4 Stellen vor dem Komma. */
const PRICE_PATTERN = /^\d{1,4}([.,]\d{1,2})?$/;

/**
 * Wandelt eine Preis-Eingabe in eine Zahl um. Deutsche Nutzer tippen ein Komma,
 * das Backend erwartet einen Punkt. Ungültige Eingaben → null.
 */
export function parsePrice(input: string | number | null | undefined): number | null {
  if (input == null) {
    return null;
  }
  const text = String(input).trim();
  if (!PRICE_PATTERN.test(text)) {
    return null;
  }
  return Number(text.replace(',', '.'));
}

/** Zeigt einen Preis im Eingabefeld so, wie ein deutscher Nutzer ihn tippen würde: "34,90". */
export function priceForInput(price: number): string {
  return price.toFixed(2).replace('.', ',');
}

/** Bestand muss eine ganze Zahl ab 0 sein. Ungültig → null. */
export function parseStock(input: string | number | null | undefined): number | null {
  if (input == null) {
    return null;
  }
  const text = String(input).trim();
  if (!/^\d{1,7}$/.test(text)) {
    return null;
  }
  const value = Number(text);
  return value <= 1_000_000 ? value : null;
}

export interface StockStatusView {
  label: string;
  /** CSS-Klasse für die Farbe. Die Bedeutung steht immer auch im Text. */
  cssClass: string;
  symbol: string;
}

const STOCK_VIEWS: Record<StockStatus, StockStatusView> = {
  IN_STOCK: { label: 'Auf Lager', cssClass: 'stock-ok', symbol: '✓' },
  LOW_STOCK: { label: 'Nur noch wenige verfügbar', cssClass: 'stock-low', symbol: '!' },
  SOLD_OUT: { label: 'Ausverkauft', cssClass: 'stock-out', symbol: '✕' },
};

/**
 * Anzeige des Bestands. Mit Stückzahl wird aus "Nur noch wenige verfügbar" ein genaues
 * "Nur noch 3 verfügbar" (offener Punkt aus 0002, in Sprint 2 geklärt).
 */
export function stockView(status: StockStatus, stock?: number): StockStatusView {
  const view = STOCK_VIEWS[status];
  if (status === 'LOW_STOCK' && stock != null && stock > 0) {
    return { ...view, label: `Nur noch ${stock} verfügbar` };
  }
  return view;
}

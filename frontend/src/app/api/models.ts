// Antwortformen der REST-API. Entsprechen den Java-Records im Backend
// (ProductResponse, CategoryResponse, PageResponse, FieldMessage).

/** Kundensicht auf den Bestand, berechnet im Backend (StockStatus.java). */
export type StockStatus = 'IN_STOCK' | 'LOW_STOCK' | 'SOLD_OUT';

export interface Product {
  id: number;
  name: string;
  description: string | null;
  price: number;
  /** Genauer Bestand, nur für die Pflegeseite. Kunden sehen stockStatus. */
  stock: number;
  stockStatus: StockStatus;
  imageUrl: string | null;
  categoryId: number;
  categoryName: string;
}

/** Was beim Anlegen (POST) oder Ändern des ganzen Produkts (PUT) geschickt wird. */
export interface ProductInput {
  name: string;
  description: string | null;
  price: number;
  stock: number;
  imageUrl: string | null;
  categoryId: number;
}

export interface Category {
  id: number;
  name: string;
  productCount: number;
}

/** Eine Ladung Produkte beim endlosen Scrollen. */
export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  last: boolean;
}

export interface FieldMessage {
  field: string;
  message: string;
}

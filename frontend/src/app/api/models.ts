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

// ---- Sprint 2: Konto, Warenkorb, Bestellungen ----

/** Sortierungen der Produktliste, gleiche Werte wie der Parameter "sort" im Backend. */
export type ProductSort = 'name' | 'price-asc' | 'price-desc' | 'newest';

export type Role = 'CUSTOMER' | 'ADMIN';

/** Angemeldeter Nutzer (UserResponse). Das Passwort verlässt das Backend nie. */
export interface User {
  id: number;
  email: string;
  name: string;
  role: Role;
}

export type OrderStatus = 'RECEIVED' | 'PROCESSING' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';

export interface OrderLine {
  productId: number;
  productName: string;
  /** Preis zum Zeitpunkt der Bestellung, vom Server festgelegt (B3). */
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface Order {
  id: number;
  createdAt: string;
  updatedAt: string;
  status: OrderStatus;
  total: number;
  itemCount: number;
  customer: { id: number; name: string; email: string };
  lines: OrderLine[];
  /** Was der aktuelle Nutzer als Nächstes setzen darf; rechnet das Backend aus. */
  allowedNextStatuses: OrderStatus[];
}

/** Was beim Bestellen geschickt wird: nur Produkt und Menge, nie ein Preis (B3). */
export interface OrderItem {
  productId: number;
  quantity: number;
}

/** Teil der 409-Antwort, wenn etwas nicht (mehr) genug auf Lager ist (B2). */
export interface StockProblem {
  productId: number;
  productName: string;
  requested: number;
  available: number;
}

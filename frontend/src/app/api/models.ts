// Antwortformen der REST-API. Entsprechen den Java-Records im Backend
// (ProductResponse, CategoryResponse, PageResponse, FieldMessage).

/** Kundensicht auf den Bestand, berechnet im Backend (StockStatus.java). */
export type StockStatus = 'IN_STOCK' | 'LOW_STOCK' | 'SOLD_OUT';

/** Darreichungsform (ProductForm.java, docs/anforderungen/0019). */
export type ProductForm = 'PULVER' | 'RIEGEL' | 'GETRAENK' | 'KAPSELN' | 'GEL';

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
  /** Packungsgröße, z. B. "1 kg" oder "330 ml" (0018). */
  size: string | null;
  /** null nur bei älteren Produkten, die noch keine Art haben. */
  form: ProductForm | null;
  /** Normaler Preis. `price` ist der Preis, den Kunden heute zahlen (im Sale niedriger, G1). */
  regularPrice: number;
  onSale: boolean;
  /** Ersparnis in ganzen Prozent, 0 ohne Sale */
  savingPercent: number;
  salePrice: number | null;
  saleFrom: string | null;
  saleUntil: string | null;
  /** Rückläufer aus einer Retoure, wieder im Verkauf (F2) */
  returnedGoods: boolean;
  /** Barcode, über den die Nährwerte aus Open Food Facts kommen (H1). */
  ean: string | null;
}

/** Nährwerte pro 100 g aus Open Food Facts (H1, docs/anforderungen/0027). Fehlende Werte sind null. */
export interface Nutrition {
  ean: string;
  productName: string | null;
  brands: string | null;
  energyKcal: number | null;
  protein: number | null;
  carbohydrates: number | null;
  sugars: number | null;
  fat: number | null;
  saturatedFat: number | null;
  salt: number | null;
  nutriScore: string | null;
  sourceUrl: string;
  source: string;
  fetchedAt: string;
  /** true: Open Food Facts war nicht erreichbar, das sind die zuletzt gespeicherten Werte (H2). */
  stale: boolean;
}

/** Sale setzen (G1): Preis und Zeitraum, beide Tage zählen mit (yyyy-MM-dd). */
export interface SaleInput {
  salePrice: number;
  from: string;
  until: string;
}

/** Rabattcode (G2), Sicht des Admins. */
export interface DiscountCode {
  id: number;
  code: string;
  percent: number;
  validFrom: string;
  validUntil: string;
  maxUses: number | null;
  timesUsed: number;
  active: boolean;
  usableToday: boolean;
}

export interface DiscountCodeInput {
  code: string;
  percent: number;
  validFrom: string;
  validUntil: string;
  maxUses: number | null;
}

/** Antwort beim Prüfen eines Codes im Warenkorb. */
export interface DiscountCheck {
  code: string;
  percent: number;
  validUntil: string;
}

/** Was beim Anlegen (POST) oder Ändern des ganzen Produkts (PUT) geschickt wird. */
export interface ProductInput {
  name: string;
  description: string | null;
  price: number;
  stock: number;
  imageUrl: string | null;
  categoryId: number;
  size: string | null;
  form: ProductForm;
  ean: string | null;
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
export type ProductSort = 'name' | 'price-asc' | 'price-desc' | 'newest' | 'saving';

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

/** Lieferadresse (0017). Nur Deutschland, deshalb ohne Land. */
export interface DeliveryAddress {
  name: string;
  street: string;
  postalCode: string;
  city: string;
}

/** Im Konto gespeicherte Adresse (Sprint 3, E2). */
export interface SavedAddress extends DeliveryAddress {
  id: number;
  /** z. B. "Zuhause" oder "Arbeit", optional */
  label: string | null;
  /** ist beim Bestellen vorausgewählt */
  standard: boolean;
}

export type SavedAddressInput = DeliveryAddress & { label: string | null; standard?: boolean };

export interface Order {
  id: number;
  createdAt: string;
  updatedAt: string;
  status: OrderStatus;
  total: number;
  itemCount: number;
  customer: { id: number; name: string; email: string };
  /** null bei Bestellungen von vor Sprint 3. */
  deliveryAddress: DeliveryAddress | null;
  lines: OrderLine[];
  /** Was der aktuelle Nutzer als Nächstes setzen darf; rechnet das Backend aus. */
  allowedNextStatuses: OrderStatus[];
  /** Summe der Positionen vor Rabatt; total ist schon abzüglich Rabatt (G2). */
  subtotal: number;
  discountCode: string | null;
  discountPercent: number | null;
  discountAmount: number | null;
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

// ---- Sprint 3: Retouren (F1, F2) ----

export type ReturnStatus = 'REQUESTED' | 'RECEIVED' | 'REFUNDED' | 'REJECTED';
export type ItemCondition = 'UNUSED' | 'USED';

export interface ReturnLine {
  productId: number;
  productName: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
  /** null, bis der Shop die Ware geprüft hat */
  condition: ItemCondition | null;
}

export interface ProductReturn {
  id: number;
  orderId: number;
  createdAt: string;
  updatedAt: string;
  status: ReturnStatus;
  reason: string;
  /** Notiz des Shops, z. B. warum abgelehnt */
  note: string | null;
  /** erst nach der Erstattung gesetzt */
  refundAmount: number | null;
  /** was voraussichtlich erstattet wird (Preise aus der Bestellung, abzüglich Rabattcode) */
  expectedRefund: number;
  customerName: string;
  lines: ReturnLine[];
  allowedNextStatuses: ReturnStatus[];
}

export interface ReturnInput {
  orderId: number;
  reason: string;
  items: { productId: number; quantity: number }[];
}

export interface ReturnUpdate {
  status: ReturnStatus;
  conditions?: { productId: number; condition: ItemCondition }[];
  note?: string;
}

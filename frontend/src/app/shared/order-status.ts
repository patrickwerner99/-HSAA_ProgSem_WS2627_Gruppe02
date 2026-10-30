import { OrderStatus } from '../api/models';

/** Gleiche Bezeichnungen wie im Backend (OrderStatus.java). */
export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  RECEIVED: 'Eingegangen',
  PROCESSING: 'In Bearbeitung',
  SHIPPED: 'Versendet',
  DELIVERED: 'Zugestellt',
  CANCELLED: 'Storniert',
};

export const ORDER_STATUSES = Object.keys(ORDER_STATUS_LABELS) as OrderStatus[];

export function isOrderStatus(value: string | null | undefined): value is OrderStatus {
  return value != null && value in ORDER_STATUS_LABELS;
}

/** Seitenzahl aus der URL (?seite=2 → Index 1). Unsinn → erste Seite. */
export function pageFromUrl(value: string | null | undefined): number {
  const page = Number(value);
  return Number.isInteger(page) && page >= 1 ? page - 1 : 0;
}

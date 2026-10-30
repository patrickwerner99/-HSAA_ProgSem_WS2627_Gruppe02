package de.hsaa.shop.order;

import java.util.List;

/**
 * Lebenszyklus einer Bestellung (docs/anforderungen/0011, B4).
 *
 * <pre>
 * RECEIVED ──▶ PROCESSING ──▶ SHIPPED ──▶ DELIVERED
 *     │             │
 *     └─────────────┴──▶ CANCELLED   (legt den Bestand zurück)
 * </pre>
 *
 * Nur diese Übergänge sind erlaubt. Eine versendete Bestellung kann nicht mehr storniert
 * werden, eine zugestellte oder stornierte gar nicht mehr geändert.
 */
public enum OrderStatus {

	RECEIVED("Eingegangen"),
	PROCESSING("In Bearbeitung"),
	SHIPPED("Versendet"),
	DELIVERED("Zugestellt"),
	CANCELLED("Storniert");

	private final String label;

	OrderStatus(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

	/** Wohin darf es von hier aus gehen? */
	public List<OrderStatus> next() {
		return switch (this) {
			case RECEIVED -> List.of(PROCESSING, CANCELLED);
			case PROCESSING -> List.of(SHIPPED, CANCELLED);
			case SHIPPED -> List.of(DELIVERED);
			case DELIVERED, CANCELLED -> List.of();
		};
	}

	public boolean canChangeTo(OrderStatus target) {
		return next().contains(target);
	}
}

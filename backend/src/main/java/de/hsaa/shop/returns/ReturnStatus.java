package de.hsaa.shop.returns;

import java.util.List;

/**
 * Wie weit eine Retoure ist (Sprint 3, F1/F2, docs/anforderungen/0025, 0026).
 *
 * <pre>
 * REQUESTED ──▶ RECEIVED ──▶ REFUNDED
 *     │             │
 *     └─────────────┴──▶ REJECTED
 * </pre>
 *
 * REQUESTED: Kunde hat die Retoure angemeldet. RECEIVED: Ware ist da und geprüft, originalverschlossene Teile
 * sind wieder im Verkauf (F2). REFUNDED: Geld ist erstattet. REJECTED: abgelehnt, z. B. Ware kam nie an.
 */
public enum ReturnStatus {

	REQUESTED("Angemeldet"),
	RECEIVED("Eingegangen und geprüft"),
	REFUNDED("Erstattet"),
	REJECTED("Abgelehnt");

	private final String label;

	ReturnStatus(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

	public List<ReturnStatus> next() {
		return switch (this) {
			case REQUESTED -> List.of(RECEIVED, REJECTED);
			case RECEIVED -> List.of(REFUNDED, REJECTED);
			case REFUNDED, REJECTED -> List.of();
		};
	}

	public boolean canChangeTo(ReturnStatus target) {
		return next().contains(target);
	}
}

package de.hsaa.shop.product;

/**
 * Was Kunden über den Bestand erfahren (docs/anforderungen/0002).
 * Die Schwelle steht nur hier, damit Frontend und Backend nie unterschiedlich rechnen.
 */
public enum StockStatus {

	/** Mehr als {@link #LOW_STOCK_LIMIT} Stück. */
	IN_STOCK,
	/** 1 bis {@link #LOW_STOCK_LIMIT} Stück. */
	LOW_STOCK,
	/** 0 Stück. */
	SOLD_OUT;

	public static final int LOW_STOCK_LIMIT = 5;

	public static StockStatus of(int stock) {
		if (stock <= 0) {
			return SOLD_OUT;
		}
		if (stock <= LOW_STOCK_LIMIT) {
			return LOW_STOCK;
		}
		return IN_STOCK;
	}
}

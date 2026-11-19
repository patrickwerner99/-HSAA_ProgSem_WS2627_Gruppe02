package de.hsaa.shop.returns;

/** Zustand einer zurückgeschickten Position, vom Admin beim Eingang festgestellt (F2). */
public enum ItemCondition {
	/** Originalverschlossen, Siegel intakt: kommt als reduzierte Ware wieder in den Verkauf. */
	UNUSED,
	/** Geöffnet oder beschädigt: wird aus Hygienegründen nicht wieder verkauft (Lebensmittel). */
	USED
}

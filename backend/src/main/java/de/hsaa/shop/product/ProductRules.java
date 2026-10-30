package de.hsaa.shop.product;

/** Gemeinsame Regeln und Meldungen für {@link ProductRequest} und {@link ProductPatch}. */
final class ProductRules {

	/** Leer, eine http(s)-Adresse oder ein mitgeliefertes Bild unter /images/. */
	static final String IMAGE_URL_PATTERN = "^(https?://\\S+|/images/[A-Za-z0-9._-]+)?$";

	/** Mindestens ein Zeichen, das kein Leerzeichen ist. */
	static final String NOT_BLANK_PATTERN = "(?s).*\\S.*";

	static final String NAME_REQUIRED = "Bitte einen Namen eingeben.";
	static final String NAME_SIZE = "Der Name muss 2 bis 100 Zeichen lang sein.";
	static final String DESCRIPTION_SIZE = "Die Beschreibung darf höchstens 1.000 Zeichen lang sein.";
	static final String PRICE_REQUIRED = "Bitte einen Preis eingeben.";
	static final String PRICE_POSITIVE = "Der Preis muss größer als 0 sein.";
	static final String PRICE_MAX = "Der Preis darf höchstens 9.999,99 € betragen.";
	static final String PRICE_DIGITS = "Der Preis darf höchstens zwei Nachkommastellen haben.";
	static final String STOCK_REQUIRED = "Bitte einen Bestand eingeben.";
	static final String STOCK_WHOLE = "Der Bestand muss eine ganze Zahl ab 0 sein.";
	static final String STOCK_MAX = "Der Bestand darf höchstens 1.000.000 betragen.";
	static final String IMAGE_URL = "Bitte eine gültige Bild-Adresse (http/https) eingeben oder das Feld leer lassen.";
	static final String CATEGORY_REQUIRED = "Bitte eine Kategorie auswählen.";

	private ProductRules() {
	}
}

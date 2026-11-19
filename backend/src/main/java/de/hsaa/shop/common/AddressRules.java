package de.hsaa.shop.common;

/**
 * Gemeinsame Regeln für Adressen, damit die Lieferadresse beim Bestellen (0017) und die gespeicherten
 * Adressen im Konto (0021) genau gleich geprüft werden. Geliefert wird nur innerhalb Deutschlands.
 */
public final class AddressRules {

	/** Buchstaben und irgendwo eine Ziffer: "Beethovenstraße 1", "Am Markt 3a". */
	public static final String STREET_PATTERN = "(?s).*\\p{L}.*\\d.*";
	public static final String POSTAL_CODE_PATTERN = "\\d{5}";

	public static final String NAME_REQUIRED = "Bitte gib den Namen des Empfängers an.";
	public static final String NAME_SIZE = "Der Name muss 2 bis 100 Zeichen lang sein.";
	public static final String STREET_REQUIRED = "Bitte gib Straße und Hausnummer an.";
	public static final String STREET_SIZE = "Straße und Hausnummer dürfen höchstens 100 Zeichen lang sein.";
	public static final String STREET_NUMBER = "Bitte gib Straße und Hausnummer an, z. B. Beethovenstraße 1.";
	public static final String POSTAL_CODE_REQUIRED = "Bitte gib die Postleitzahl an.";
	public static final String POSTAL_CODE_FORMAT = "Die Postleitzahl hat genau 5 Ziffern.";
	public static final String CITY_REQUIRED = "Bitte gib den Ort an.";
	public static final String CITY_SIZE = "Der Ort muss 2 bis 60 Zeichen lang sein.";

	private AddressRules() {
	}
}

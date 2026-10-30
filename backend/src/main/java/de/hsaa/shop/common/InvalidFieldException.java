package de.hsaa.shop.common;

/**
 * Ein Feld ist formal richtig, der Wert passt aber nicht, z. B. eine Kategorie-ID,
 * die es nicht gibt → HTTP 400 mit Feldfehler, wie bei der normalen Validierung.
 */
public class InvalidFieldException extends RuntimeException {

	private final String field;

	public InvalidFieldException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String getField() {
		return field;
	}
}

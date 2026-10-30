package de.hsaa.shop.common;

/**
 * Die Eingabe ist für sich gültig, widerspricht aber dem aktuellen Datenbestand → HTTP 409.
 * Beispiele: Kategoriename doppelt, Kategorie mit Produkten löschen.
 */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}
}

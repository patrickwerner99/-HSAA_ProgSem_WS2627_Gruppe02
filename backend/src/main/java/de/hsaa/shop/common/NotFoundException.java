package de.hsaa.shop.common;

/** Etwas existiert nicht (mehr) → HTTP 404. Die Meldung wird dem Nutzer so angezeigt. */
public class NotFoundException extends RuntimeException {

	public NotFoundException(String message) {
		super(message);
	}
}

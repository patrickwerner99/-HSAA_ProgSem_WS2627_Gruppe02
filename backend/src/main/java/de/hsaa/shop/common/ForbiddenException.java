package de.hsaa.shop.common;

/** Angemeldet, aber diese Aktion ist nicht erlaubt → HTTP 403. */
public class ForbiddenException extends RuntimeException {

	public ForbiddenException(String message) {
		super(message);
	}
}

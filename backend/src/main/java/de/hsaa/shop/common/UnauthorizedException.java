package de.hsaa.shop.common;

/** Nicht (richtig) angemeldet → HTTP 401. */
public class UnauthorizedException extends RuntimeException {

	public UnauthorizedException(String message) {
		super(message);
	}
}

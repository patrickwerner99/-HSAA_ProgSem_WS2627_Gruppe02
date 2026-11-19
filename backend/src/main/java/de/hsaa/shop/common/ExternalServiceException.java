package de.hsaa.shop.common;

/** Ein fremder Dienst (z. B. Open Food Facts) antwortet nicht oder zu langsam → 503 (H2). */
public class ExternalServiceException extends RuntimeException {

	public ExternalServiceException(String message) {
		super(message);
	}
}

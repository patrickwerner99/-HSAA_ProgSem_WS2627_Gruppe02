package de.hsaa.shop.common;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import de.hsaa.shop.order.OutOfStockException;

/**
 * Übersetzt jeden Fehler in eine einheitliche Antwort im Format "Problem Details" (RFC 9457):
 * <pre>
 * { "status": 400, "title": "Ungültige Eingabe", "detail": "...",
 *   "errors": [ { "field": "price", "message": "Der Preis muss größer als 0 sein." } ] }
 * </pre>
 * Nutzer sehen nur {@code detail} bzw. die Feldmeldungen, nie technische Details
 * (docs/anforderungen/0005, ADR-0003).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/** Bean Validation (@NotBlank, @DecimalMin, ...) ist fehlgeschlagen. */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
		List<FieldMessage> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> new FieldMessage(error.getField(), error.getDefaultMessage()))
				.toList();
		return invalidInput(errors);
	}

	@ExceptionHandler(InvalidFieldException.class)
	public ProblemDetail handleInvalidField(InvalidFieldException ex) {
		return invalidInput(List.of(new FieldMessage(ex.getField(), ex.getMessage())));
	}

	/** Kaputtes JSON oder falscher Typ, z. B. "abc" als Preis. */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ProblemDetail handleUnreadable(HttpMessageNotReadableException ex) {
		return problem(HttpStatus.BAD_REQUEST, "Ungültige Eingabe",
				"Die Eingabe enthält ungültige Werte. Bitte prüfe Preis und Bestand.");
	}

	/** z. B. /api/products/abc statt einer Zahl. */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		return problem(HttpStatus.BAD_REQUEST, "Ungültige Anfrage",
				"Der Wert für \"" + ex.getName() + "\" ist ungültig.");
	}

	@ExceptionHandler(NotFoundException.class)
	public ProblemDetail handleNotFound(NotFoundException ex) {
		return problem(HttpStatus.NOT_FOUND, "Nicht gefunden", ex.getMessage());
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ProblemDetail handleNoResource(NoResourceFoundException ex) {
		return problem(HttpStatus.NOT_FOUND, "Nicht gefunden", "Diese Adresse gibt es nicht.");
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ProblemDetail handleMethod(HttpRequestMethodNotSupportedException ex) {
		return problem(HttpStatus.METHOD_NOT_ALLOWED, "Nicht erlaubt",
				"Diese Aktion ist hier nicht möglich.");
	}

	@ExceptionHandler(UnauthorizedException.class)
	public ProblemDetail handleUnauthorized(UnauthorizedException ex) {
		return problem(HttpStatus.UNAUTHORIZED, "Nicht angemeldet", ex.getMessage());
	}

	@ExceptionHandler(ForbiddenException.class)
	public ProblemDetail handleForbidden(ForbiddenException ex) {
		return problem(HttpStatus.FORBIDDEN, "Keine Berechtigung", ex.getMessage());
	}

	/** Nicht genug auf Lager (B2): Meldung plus Liste der betroffenen Produkte unter "items". */
	@ExceptionHandler(OutOfStockException.class)
	public ProblemDetail handleOutOfStock(OutOfStockException ex) {
		ProblemDetail problem = problem(HttpStatus.CONFLICT, "Nicht genug auf Lager", ex.getMessage());
		problem.setProperty("items", ex.getProblems());
		return problem;
	}

	/** Bild größer als spring.servlet.multipart.max-file-size. */
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ProblemDetail handleTooLarge(MaxUploadSizeExceededException ex) {
		return problem(HttpStatus.CONTENT_TOO_LARGE, "Datei zu groß", "Das Bild ist zu groß. Erlaubt sind höchstens 5 MB.");
	}

	@ExceptionHandler(MissingServletRequestPartException.class)
	public ProblemDetail handleMissingPart(MissingServletRequestPartException ex) {
		return invalidInput(List.of(new FieldMessage(ex.getRequestPartName(), "Bitte ein Bild auswählen.")));
	}

	/** Externe API (Open Food Facts) nicht erreichbar: 503, der Rest des Shops funktioniert weiter (H2). */
	@ExceptionHandler(ExternalServiceException.class)
	public ProblemDetail handleExternalService(ExternalServiceException ex) {
		return problem(HttpStatus.SERVICE_UNAVAILABLE, "Gerade nicht verfügbar", ex.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	public ProblemDetail handleConflict(ConflictException ex) {
		return problem(HttpStatus.CONFLICT, "Konflikt", ex.getMessage());
	}

	/** Absicherung, falls zwei Anfragen gleichzeitig dieselbe Regel verletzen (z. B. doppelter Name). */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ProblemDetail handleIntegrity(DataIntegrityViolationException ex) {
		log.warn("Datenbankregel verletzt", ex);
		return problem(HttpStatus.CONFLICT, "Konflikt",
				"Die Änderung passt nicht zum aktuellen Datenbestand. Bitte lade die Seite neu.");
	}

	/** Alles Unerwartete: Details nur ins Log, der Nutzer bekommt eine allgemeine Meldung. */
	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpected(Exception ex) {
		if (ex instanceof ErrorResponse errorResponse && errorResponse.getStatusCode().is4xxClientError()) {
			// Von Spring erkannte Anfragefehler (z. B. falscher Content-Type) behalten ihren 4xx-Status.
			ProblemDetail problem = ProblemDetail.forStatusAndDetail(errorResponse.getStatusCode(),
					"Die Anfrage konnte nicht verarbeitet werden.");
			problem.setTitle("Ungültige Anfrage");
			return problem;
		}
		log.error("Unerwarteter Fehler", ex);
		return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Serverfehler",
				"Da ist etwas schiefgelaufen. Bitte versuche es erneut.");
	}

	private static ProblemDetail invalidInput(List<FieldMessage> errors) {
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Ungültige Eingabe",
				"Bitte prüfe die markierten Felder.");
		problem.setProperty("errors", errors);
		return problem;
	}

	private static ProblemDetail problem(HttpStatus status, String title, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		return problem;
	}
}

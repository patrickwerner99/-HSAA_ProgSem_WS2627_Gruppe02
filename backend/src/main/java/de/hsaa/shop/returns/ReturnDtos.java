package de.hsaa.shop.returns;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Ein- und Ausgabeformate der Retouren-API (F1, F2). */
public final class ReturnDtos {

	private ReturnDtos() {
	}

	/** Retoure anmelden (Kunde): welche Bestellung, welche Teile, warum. */
	public record ReturnRequest(
			@NotNull(message = "Bitte die Bestellung angeben.")
			Long orderId,

			@NotEmpty(message = "Bitte mindestens ein Produkt auswählen.")
			List<@Valid @NotNull(message = "Ungültige Position.") ReturnItemRequest> items,

			@NotBlank(message = "Bitte einen Grund angeben.")
			@Size(max = 500, message = "Der Grund darf höchstens 500 Zeichen lang sein.")
			String reason) {
	}

	public record ReturnItemRequest(
			@NotNull(message = "Bitte ein Produkt angeben.") Long productId,
			@NotNull(message = "Bitte eine Menge angeben.")
			@Min(value = 1, message = "Die Menge muss mindestens 1 sein.")
			@Max(value = 99, message = "Höchstens 99 Stück.")
			Integer quantity) {
	}

	/**
	 * Retoure bearbeiten (Admin). Bei RECEIVED braucht jede Position einen Zustand (originalverschlossen / geöffnet).
	 * {@code note} ist optional und für den Kunden sichtbar.
	 */
	public record ReturnUpdateRequest(
			@NotNull(message = "Bitte einen Status angeben.") ReturnStatus status,
			List<@Valid @NotNull ConditionRequest> conditions,
			@Size(max = 500, message = "Die Notiz darf höchstens 500 Zeichen lang sein.") String note) {
	}

	public record ConditionRequest(
			@NotNull(message = "Bitte ein Produkt angeben.") Long productId,
			@NotNull(message = "Bitte den Zustand angeben.") ItemCondition condition) {
	}

	public record ReturnLineResponse(Long productId, String productName, BigDecimal unitPrice, int quantity,
			BigDecimal lineTotal, ItemCondition condition) {
	}

	public record ReturnResponse(Long id, Long orderId, Instant createdAt, Instant updatedAt, ReturnStatus status,
			String reason, String note, BigDecimal refundAmount, BigDecimal expectedRefund, String customerName,
			List<ReturnLineResponse> lines, List<ReturnStatus> allowedNextStatuses) {
	}
}

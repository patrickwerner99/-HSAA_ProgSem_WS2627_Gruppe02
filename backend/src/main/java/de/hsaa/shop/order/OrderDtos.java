package de.hsaa.shop.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Ein- und Ausgabeformate der Bestell-API. */
public final class OrderDtos {

	private OrderDtos() {
	}

	/**
	 * Bestellung aufgeben: nur Produkt-IDs und Mengen. Schickt der Browser trotzdem Preise mit,
	 * werden sie ignoriert, Preise bestimmt der Shop (B3).
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record OrderRequest(
			@NotEmpty(message = "Der Warenkorb ist leer.")
			@Size(max = 50, message = "Höchstens 50 verschiedene Produkte pro Bestellung.")
			List<@Valid @NotNull(message = "Ungültige Position.") OrderItemRequest> items) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record OrderItemRequest(
			@NotNull(message = "Bitte ein Produkt angeben.") Long productId,
			@NotNull(message = "Bitte eine Menge angeben.")
			@Min(value = 1, message = "Die Menge muss mindestens 1 sein.")
			@Max(value = 99, message = "Höchstens 99 Stück pro Produkt.")
			Integer quantity) {
	}

	public record StatusChangeRequest(@NotNull(message = "Bitte einen Status angeben.") OrderStatus status) {
	}

	public record CustomerInfo(Long id, String name, String email) {
	}

	public record OrderLineResponse(Long productId, String productName, BigDecimal unitPrice, int quantity,
			BigDecimal lineTotal) {
	}

	/**
	 * {@code allowedNextStatuses} sagt dem Frontend, welche Status-Knöpfe es anbieten soll,
	 * passend zur Rolle des Betrachters. So steht die Regel nur einmal im Backend.
	 */
	public record OrderResponse(Long id, Instant createdAt, Instant updatedAt, OrderStatus status, BigDecimal total,
			int itemCount, CustomerInfo customer, List<OrderLineResponse> lines,
			List<OrderStatus> allowedNextStatuses) {
	}

	/** Ein Produkt, das nicht (mehr) in der gewünschten Menge da ist. */
	public record StockProblem(Long productId, String productName, int requested, int available) {
	}
}

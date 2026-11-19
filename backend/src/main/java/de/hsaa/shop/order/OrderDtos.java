package de.hsaa.shop.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.hsaa.shop.common.AddressRules;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Ein- und Ausgabeformate der Bestell-API. */
public final class OrderDtos {

	private OrderDtos() {
	}

	/**
	 * Bestellung aufgeben: Produkt-IDs, Mengen und die Lieferadresse (0017). Die Adresse kommt entweder
	 * als gespeicherte Adresse aus dem Konto ({@code addressId}, 0021) oder direkt eingegeben
	 * ({@code deliveryAddress}). Schickt der Browser Preise mit, werden sie ignoriert (B3).
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record OrderRequest(
			@NotEmpty(message = "Der Warenkorb ist leer.")
			@Size(max = 50, message = "Höchstens 50 verschiedene Produkte pro Bestellung.")
			List<@Valid @NotNull(message = "Ungültige Position.") OrderItemRequest> items,

			@Valid
			DeliveryAddressRequest deliveryAddress,

			Long addressId,

			/** optionaler Rabattcode (G2), Groß-/Kleinschreibung egal */
			String discountCode) {
	}

	/**
	 * Lieferadresse beim Bestellen (0017). Nur Deutschland, deshalb genau fünf Ziffern als PLZ.
	 * Die Straße braucht eine Hausnummer, sonst kommt das Paket nicht an.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record DeliveryAddressRequest(
			@NotBlank(message = AddressRules.NAME_REQUIRED)
			@Size(min = 2, max = 100, message = AddressRules.NAME_SIZE)
			String name,

			@NotBlank(message = AddressRules.STREET_REQUIRED)
			@Size(max = 100, message = AddressRules.STREET_SIZE)
			@Pattern(regexp = AddressRules.STREET_PATTERN, message = AddressRules.STREET_NUMBER)
			String street,

			@NotBlank(message = AddressRules.POSTAL_CODE_REQUIRED)
			@Pattern(regexp = AddressRules.POSTAL_CODE_PATTERN, message = AddressRules.POSTAL_CODE_FORMAT)
			String postalCode,

			@NotBlank(message = AddressRules.CITY_REQUIRED)
			@Size(min = 2, max = 60, message = AddressRules.CITY_SIZE)
			String city) {

		DeliveryAddress toAddress() {
			return new DeliveryAddress(name.trim(), street.trim(), postalCode.trim(), city.trim());
		}
	}

	public record DeliveryAddressResponse(String name, String street, String postalCode, String city) {

		static DeliveryAddressResponse from(DeliveryAddress address) {
			return address == null ? null
					: new DeliveryAddressResponse(address.getName(), address.getStreet(), address.getPostalCode(),
							address.getCity());
		}
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
			int itemCount, CustomerInfo customer, DeliveryAddressResponse deliveryAddress,
			List<OrderLineResponse> lines, List<OrderStatus> allowedNextStatuses,
			BigDecimal subtotal, String discountCode, Integer discountPercent, BigDecimal discountAmount) {
	}

	/** Ein Produkt, das nicht (mehr) in der gewünschten Menge da ist. */
	public record StockProblem(Long productId, String productName, int requested, int available) {
	}
}

package de.hsaa.shop.product;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/**
 * Ein Produkt für einen Zeitraum in den Sale stellen (Sprint 3, G1, docs/anforderungen/0023).
 * Beide Tage zählen mit: von 01.12. bis 07.12. heißt eine ganze Woche.
 * Dass der Sale-Preis unter dem normalen Preis liegt und das Ende nicht in der Vergangenheit,
 * prüft {@link ProductService#startSale}, weil dafür das Produkt bzw. das heutige Datum nötig ist.
 */
public record SaleRequest(
		@NotNull(message = "Bitte einen Sale-Preis eingeben.")
		@DecimalMin(value = "0.01", message = ProductRules.PRICE_POSITIVE)
		@Digits(integer = 4, fraction = 2, message = ProductRules.PRICE_DIGITS)
		BigDecimal salePrice,

		@NotNull(message = "Bitte angeben, ab wann der Sale gilt.")
		LocalDate from,

		@NotNull(message = "Bitte angeben, bis wann der Sale gilt.")
		LocalDate until) {
}

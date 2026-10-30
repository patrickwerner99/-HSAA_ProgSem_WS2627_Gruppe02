package de.hsaa.shop.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Komplettes Produkt beim Anlegen (POST) und beim Ändern des ganzen Produkts (PUT).
 * Die Annotationen sind die verbindlichen Regeln aus docs/anforderungen/0003 (ADR-0003).
 * Der Bestand ist ein BigDecimal, damit "3.5" abgelehnt statt still auf 3 abgerundet wird.
 */
public record ProductRequest(
		@NotBlank(message = ProductRules.NAME_REQUIRED)
		@Size(min = 2, max = 100, message = ProductRules.NAME_SIZE)
		String name,

		@Size(max = 1000, message = ProductRules.DESCRIPTION_SIZE)
		String description,

		@NotNull(message = ProductRules.PRICE_REQUIRED)
		@DecimalMin(value = "0.01", message = ProductRules.PRICE_POSITIVE)
		@DecimalMax(value = "9999.99", message = ProductRules.PRICE_MAX)
		@Digits(integer = 4, fraction = 2, message = ProductRules.PRICE_DIGITS)
		BigDecimal price,

		@NotNull(message = ProductRules.STOCK_REQUIRED)
		@DecimalMin(value = "0", message = ProductRules.STOCK_WHOLE)
		@DecimalMax(value = "1000000", message = ProductRules.STOCK_MAX)
		@Digits(integer = 7, fraction = 0, message = ProductRules.STOCK_WHOLE)
		BigDecimal stock,

		@Size(max = 500, message = ProductRules.IMAGE_URL)
		@Pattern(regexp = ProductRules.IMAGE_URL_PATTERN, message = ProductRules.IMAGE_URL)
		String imageUrl,

		@NotNull(message = ProductRules.CATEGORY_REQUIRED)
		Long categoryId) {
}

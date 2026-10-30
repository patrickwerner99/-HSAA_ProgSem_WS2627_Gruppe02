package de.hsaa.shop.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Änderung einzelner Felder (PATCH). Felder, die fehlen oder null sind, bleiben unverändert.
 * Beschreibung und Bild-URL lassen sich mit einem leeren Text "" löschen.
 * Es gelten dieselben Regeln wie in {@link ProductRequest}, nur ist nichts Pflicht.
 */
public record ProductPatch(
		@Size(min = 2, max = 100, message = ProductRules.NAME_SIZE)
		@Pattern(regexp = ProductRules.NOT_BLANK_PATTERN, message = ProductRules.NAME_REQUIRED)
		String name,

		@Size(max = 1000, message = ProductRules.DESCRIPTION_SIZE)
		String description,

		@DecimalMin(value = "0.01", message = ProductRules.PRICE_POSITIVE)
		@DecimalMax(value = "9999.99", message = ProductRules.PRICE_MAX)
		@Digits(integer = 4, fraction = 2, message = ProductRules.PRICE_DIGITS)
		BigDecimal price,

		@DecimalMin(value = "0", message = ProductRules.STOCK_WHOLE)
		@DecimalMax(value = "1000000", message = ProductRules.STOCK_MAX)
		@Digits(integer = 7, fraction = 0, message = ProductRules.STOCK_WHOLE)
		BigDecimal stock,

		@Size(max = 500, message = ProductRules.IMAGE_URL)
		@Pattern(regexp = ProductRules.IMAGE_URL_PATTERN, message = ProductRules.IMAGE_URL)
		String imageUrl,

		Long categoryId) {
}

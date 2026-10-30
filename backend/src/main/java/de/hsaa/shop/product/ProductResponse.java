package de.hsaa.shop.product;

import java.math.BigDecimal;

/**
 * Antwort der API für ein Produkt. {@code stockStatus} ist für die Kundenansicht,
 * {@code stock} (genaue Zahl) für die Pflegeseite.
 */
public record ProductResponse(
		Long id,
		String name,
		String description,
		BigDecimal price,
		int stock,
		StockStatus stockStatus,
		String imageUrl,
		Long categoryId,
		String categoryName) {

	static ProductResponse from(Product product) {
		return new ProductResponse(
				product.getId(),
				product.getName(),
				product.getDescription(),
				product.getPrice(),
				product.getStock(),
				StockStatus.of(product.getStock()),
				product.getImageUrl(),
				product.getCategory().getId(),
				product.getCategory().getName());
	}
}

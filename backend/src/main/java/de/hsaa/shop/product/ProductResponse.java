package de.hsaa.shop.product;

import java.math.BigDecimal;
import java.time.LocalDate;

import de.hsaa.shop.common.ShopTime;

/**
 * Antwort der API für ein Produkt. {@code stockStatus} ist für die Kundenansicht,
 * {@code stock} (genaue Zahl) für die Pflegeseite.
 * {@code price} ist immer der Preis, den Kunden heute zahlen (im Sale der Sale-Preis, G1).
 * {@code regularPrice} ist der normale Preis, den der Admin pflegt; im Sale steht er durchgestrichen daneben.
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
		String categoryName,
		String size,
		ProductForm form,
		BigDecimal regularPrice,
		boolean onSale,
		int savingPercent,
		BigDecimal salePrice,
		LocalDate saleFrom,
		LocalDate saleUntil,
		boolean returnedGoods,
		String ean) {

	static ProductResponse from(Product product) {
		LocalDate today = ShopTime.today();
		return new ProductResponse(
				product.getId(),
				product.getName(),
				product.getDescription(),
				product.currentPrice(today),
				product.getStock(),
				StockStatus.of(product.getStock()),
				product.getImageUrl(),
				product.getCategory().getId(),
				product.getCategory().getName(),
				product.getSize(),
				product.getForm(),
				product.getPrice(),
				product.isOnSale(today),
				product.savingPercent(today),
				product.getSalePrice(),
				product.getSaleFrom(),
				product.getSaleUntil(),
				product.isReturnedGoods(),
				product.getEan());
	}
}

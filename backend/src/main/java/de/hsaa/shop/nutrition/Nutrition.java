package de.hsaa.shop.nutrition;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Nährwerte eines Produkts, wie sie von Open Food Facts kommen (Sprint 3, H1, docs/anforderungen/0027).
 * Alle Werte pro 100 g bzw. 100 ml; fehlt ein Wert bei Open Food Facts, ist er null.
 *
 * @param productName Name bei Open Food Facts; zeigt dem Kunden, woher die Werte stammen
 * @param fetchedAt wann der Shop die Daten zuletzt bei Open Food Facts geholt hat
 * @param stale true = Open Food Facts war gerade nicht erreichbar, das sind die zuletzt gespeicherten Werte (H2)
 */
public record Nutrition(
		String ean,
		String productName,
		String brands,
		BigDecimal energyKcal,
		BigDecimal protein,
		BigDecimal carbohydrates,
		BigDecimal sugars,
		BigDecimal fat,
		BigDecimal saturatedFat,
		BigDecimal salt,
		String nutriScore,
		String sourceUrl,
		String source,
		Instant fetchedAt,
		boolean stale) {

	Nutrition asStale() {
		return new Nutrition(ean, productName, brands, energyKcal, protein, carbohydrates, sugars, fat, saturatedFat,
				salt, nutriScore, sourceUrl, source, fetchedAt, true);
	}
}

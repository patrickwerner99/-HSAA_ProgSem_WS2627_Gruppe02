package de.hsaa.shop.nutrition;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import de.hsaa.shop.common.ExternalServiceException;
import de.hsaa.shop.common.NotFoundException;
import de.hsaa.shop.product.ProductService;

/**
 * Nährwerte zu einem Produkt (Sprint 3, H1/H2, ADR-0012). Antworten von Open Food Facts werden 24 Stunden
 * zwischengespeichert: Das schont die fremde API und macht die Seite schnell. Ist Open Food Facts nicht
 * erreichbar, liefern wir die zuletzt gespeicherten Werte (als "stale" markiert), sonst 503.
 */
@Service
public class NutritionService {

	private static final Logger log = LoggerFactory.getLogger(NutritionService.class);
	static final Duration FRESH_FOR = Duration.ofHours(24);

	private final OpenFoodFactsClient client;
	private final ProductService products;
	private final Map<String, Optional<Nutrition>> cache = new ConcurrentHashMap<>();

	public NutritionService(OpenFoodFactsClient client, ProductService products) {
		this.client = client;
		this.products = products;
	}

	public Nutrition forProduct(Long productId) {
		String ean = products.eanOf(productId);
		if (ean == null || ean.isBlank()) {
			throw new NotFoundException("Für dieses Produkt sind keine Nährwerte hinterlegt.");
		}
		Optional<Nutrition> cached = cache.get(ean);
		if (cached != null && isFresh(cached)) {
			return cached.orElseThrow(NutritionService::unknown);
		}
		try {
			Optional<Nutrition> fetched = client.fetch(ean);
			cache.put(ean, fetched);
			return fetched.orElseThrow(NutritionService::unknown);
		}
		catch (ExternalServiceException ex) {
			log.warn("Open Food Facts nicht erreichbar für EAN {}: {}", ean, ex.getMessage());
			if (cached != null && cached.isPresent()) {
				return cached.get().asStale();
			}
			throw ex;
		}
	}

	/** Für Tests: Zwischenspeicher leeren. */
	void clearCache() {
		cache.clear();
	}

	/** Für Tests: so tun, als wären alle gespeicherten Werte älter als 24 Stunden. */
	void expireCacheForTests() {
		cache.replaceAll((ean, entry) -> entry.map(n -> new Nutrition(n.ean(), n.productName(), n.brands(),
				n.energyKcal(), n.protein(), n.carbohydrates(), n.sugars(), n.fat(), n.saturatedFat(), n.salt(),
				n.nutriScore(), n.sourceUrl(), n.source(), n.fetchedAt().minus(FRESH_FOR).minusSeconds(1), false)));
	}

	private static boolean isFresh(Optional<Nutrition> entry) {
		// Auch "unbekannt" merken wir uns, aber nur kurz, falls das Produkt bald eingetragen wird.
		Duration age = entry.map(n -> Duration.between(n.fetchedAt(), Instant.now())).orElse(Duration.ZERO);
		return entry.isEmpty() || age.compareTo(FRESH_FOR) < 0;
	}

	private static NotFoundException unknown() {
		return new NotFoundException("Open Food Facts kennt dieses Produkt nicht.");
	}
}

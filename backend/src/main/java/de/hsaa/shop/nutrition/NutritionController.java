package de.hsaa.shop.nutrition;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Nährwerte eines Produkts aus Open Food Facts (Sprint 3, H1/H2).
 *
 * <pre>
 * GET /api/products/{id}/nutrition   Nährwerte pro 100 g    200 / 404 (kein Barcode, unbekannt) / 503 (API weg)
 * </pre>
 *
 * Eigener Aufruf statt Teil von GET /api/products/{id}: So lädt die Produktseite immer sofort,
 * die Nährwerte kommen hinterher oder eben nicht (H2).
 */
@RestController
public class NutritionController {

	private final NutritionService service;

	public NutritionController(NutritionService service) {
		this.service = service;
	}

	@GetMapping("/api/products/{id}/nutrition")
	public Nutrition get(@PathVariable Long id) {
		return service.forProduct(id);
	}
}

package de.hsaa.shop.product;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.hsaa.shop.common.PageResponse;
import jakarta.validation.Valid;

/**
 * REST-Endpunkte für Produkte unter /api/products.
 *
 * <pre>
 * GET    /api/products?q=whey&amp;categoryId=2&amp;form=RIEGEL&amp;inStock=true&amp;sale=true&amp;sort=price-asc&amp;page=0&amp;size=12
 *                                                    Liste (Suche, Filter, Sortierung, endloses Scrollen)
 * GET    /api/products/{id}                                 Details
 * POST   /api/products                                      anlegen
 * PUT    /api/products/{id}                                 ganzes Produkt ändern
 * PATCH  /api/products/{id}                                 einzelne Felder ändern
 * DELETE /api/products/{id}                                 löschen
 * PUT    /api/products/{id}/sale                            in den Sale stellen (G1)
 * DELETE /api/products/{id}/sale                            Sale beenden
 * Schreibende Aufrufe nur für ADMIN (SecurityConfig).
 * </pre>
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

	private final ProductService service;

	public ProductController(ProductService service) {
		this.service = service;
	}

	@GetMapping
	public PageResponse<ProductResponse> list(
			@RequestParam(name = "q", required = false) String query,
			@RequestParam(required = false) Long categoryId,
			@RequestParam(required = false) String form,
			@RequestParam(defaultValue = "false") boolean inStock,
			@RequestParam(defaultValue = "false") boolean sale,
			@RequestParam(defaultValue = "name") String sort,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "" + ProductService.DEFAULT_PAGE_SIZE) int size) {
		return service.search(query, categoryId, form, inStock, sale, sort, page, size);
	}

	@GetMapping("/{id}")
	public ProductResponse get(@PathVariable Long id) {
		return service.findById(id);
	}

	@PostMapping
	public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
		ProductResponse created = service.create(request);
		return ResponseEntity.created(URI.create("/api/products/" + created.id())).body(created);
	}

	@PutMapping("/{id}")
	public ProductResponse replace(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
		return service.replace(id, request);
	}

	@PatchMapping("/{id}")
	public ProductResponse patch(@PathVariable Long id, @Valid @RequestBody ProductPatch patch) {
		return service.patch(id, patch);
	}

	@PutMapping("/{id}/sale")
	public ProductResponse startSale(@PathVariable Long id, @Valid @RequestBody SaleRequest request) {
		return service.startSale(id, request);
	}

	@DeleteMapping("/{id}/sale")
	public ProductResponse endSale(@PathVariable Long id) {
		return service.endSale(id);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}
}

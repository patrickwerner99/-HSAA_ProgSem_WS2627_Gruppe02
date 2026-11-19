package de.hsaa.shop.discount;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.hsaa.shop.discount.DiscountDtos.DiscountCheckResponse;
import de.hsaa.shop.discount.DiscountDtos.DiscountCodePatch;
import de.hsaa.shop.discount.DiscountDtos.DiscountCodeRequest;
import de.hsaa.shop.discount.DiscountDtos.DiscountCodeResponse;
import jakarta.validation.Valid;

/**
 * Rabattcodes unter /api/discount-codes (G2). Wer was darf, steht in der SecurityConfig.
 *
 * <pre>
 * GET    /api/discount-codes          alle Codes (Admin)                               200 / 401 / 403
 * POST   /api/discount-codes          Code anlegen (Admin)                             201 / 400 / 409
 * PATCH  /api/discount-codes/{id}     abschalten / verlängern (Admin)                  200 / 400 / 404
 * DELETE /api/discount-codes/{id}     löschen, nur wenn nie benutzt (Admin)            204 / 404 / 409
 * GET    /api/discount-codes/{code}   gilt der Code? (angemeldet, beim Bestellen)      200 / 401 / 404 / 409
 * </pre>
 */
@RestController
@RequestMapping("/api/discount-codes")
public class DiscountController {

	private final DiscountService service;

	public DiscountController(DiscountService service) {
		this.service = service;
	}

	@GetMapping
	public List<DiscountCodeResponse> list() {
		return service.list();
	}

	@PostMapping
	public ResponseEntity<DiscountCodeResponse> create(@Valid @RequestBody DiscountCodeRequest request) {
		DiscountCodeResponse created = service.create(request);
		return ResponseEntity.created(URI.create("/api/discount-codes/" + created.code())).body(created);
	}

	@PatchMapping("/{id}")
	public DiscountCodeResponse patch(@PathVariable Long id, @Valid @RequestBody DiscountCodePatch patch) {
		return service.patch(id, patch);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{code}")
	public DiscountCheckResponse check(@PathVariable String code) {
		return service.check(code);
	}
}

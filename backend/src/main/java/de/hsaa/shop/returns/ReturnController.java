package de.hsaa.shop.returns;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.hsaa.shop.common.PageResponse;
import de.hsaa.shop.returns.ReturnDtos.ReturnRequest;
import de.hsaa.shop.returns.ReturnDtos.ReturnResponse;
import de.hsaa.shop.returns.ReturnDtos.ReturnUpdateRequest;
import jakarta.validation.Valid;

/**
 * Retouren unter /api/returns (nur angemeldet, Sprint 3 F1/F2).
 *
 * <pre>
 * POST  /api/returns                          Retoure anmelden (Kunde)              201 / 400 / 401 / 404 / 409
 * GET   /api/returns?status=&amp;page=&amp;size=      Kunde: eigene, Admin: alle            200 / 401
 * GET   /api/orders/{orderId}/returns           Retouren zu einer Bestellung          200 / 401
 * GET   /api/returns/{id}                     eine Retoure                          200 / 401 / 404
 * PATCH /api/returns/{id}                     Status setzen (nur Admin)             200 / 400 / 403 / 404 / 409
 * </pre>
 */
@RestController
@RequestMapping("/api/returns")
public class ReturnController {

	private final ReturnService service;

	public ReturnController(ReturnService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<ReturnResponse> create(@Valid @RequestBody ReturnRequest request) {
		ReturnResponse created = service.create(request);
		return ResponseEntity.created(URI.create("/api/returns/" + created.id())).body(created);
	}

	@GetMapping
	public PageResponse<ReturnResponse> list(@RequestParam(required = false) ReturnStatus status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return service.list(status, page, size);
	}

	/** Retouren einer Bestellung, für die Bestellseite: GET /api/orders/{orderId}/returns. */
	@RestController
	@RequestMapping("/api/orders/{orderId}/returns")
	public static class OrderReturns {

		private final ReturnService service;

		public OrderReturns(ReturnService service) {
			this.service = service;
		}

		@GetMapping
		public List<ReturnResponse> list(@PathVariable Long orderId) {
			return service.forOrder(orderId);
		}
	}

	@GetMapping("/{id}")
	public ReturnResponse get(@PathVariable Long id) {
		return service.get(id);
	}

	@PatchMapping("/{id}")
	public ReturnResponse update(@PathVariable Long id, @Valid @RequestBody ReturnUpdateRequest request) {
		return service.update(id, request);
	}
}

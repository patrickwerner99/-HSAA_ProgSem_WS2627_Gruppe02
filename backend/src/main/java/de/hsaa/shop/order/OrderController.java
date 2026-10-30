package de.hsaa.shop.order;

import java.net.URI;

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
import de.hsaa.shop.order.OrderDtos.OrderRequest;
import de.hsaa.shop.order.OrderDtos.OrderResponse;
import de.hsaa.shop.order.OrderDtos.StatusChangeRequest;
import jakarta.validation.Valid;

/**
 * Bestellungen unter /api/orders (nur angemeldet, siehe SecurityConfig).
 *
 * <pre>
 * POST  /api/orders                       bestellen                      201 / 400 / 401 / 409
 * GET   /api/orders?status=&amp;page=&amp;size=   Kunde: eigene, Admin: alle      200 / 401
 * GET   /api/orders/{id}                  Details                        200 / 401 / 404
 * PATCH /api/orders/{id}                  Status ändern / stornieren     200 / 400 / 401 / 403 / 404 / 409
 * </pre>
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

	private final OrderService service;

	public OrderController(OrderService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<OrderResponse> place(@Valid @RequestBody OrderRequest request) {
		OrderResponse created = service.place(request);
		return ResponseEntity.created(URI.create("/api/orders/" + created.id())).body(created);
	}

	@GetMapping
	public PageResponse<OrderResponse> list(
			@RequestParam(required = false) OrderStatus status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return service.list(status, page, size);
	}

	@GetMapping("/{id}")
	public OrderResponse get(@PathVariable Long id) {
		return service.get(id);
	}

	@PatchMapping("/{id}")
	public OrderResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StatusChangeRequest request) {
		return service.changeStatus(id, request.status());
	}
}

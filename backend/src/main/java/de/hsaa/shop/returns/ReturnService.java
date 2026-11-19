package de.hsaa.shop.returns;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.hsaa.shop.account.AppUser;
import de.hsaa.shop.account.CurrentUser;
import de.hsaa.shop.account.Role;
import de.hsaa.shop.common.ConflictException;
import de.hsaa.shop.common.ForbiddenException;
import de.hsaa.shop.common.InvalidFieldException;
import de.hsaa.shop.common.NotFoundException;
import de.hsaa.shop.common.PageResponse;
import de.hsaa.shop.common.ShopTime;
import de.hsaa.shop.order.OrderLine;
import de.hsaa.shop.order.OrderStatus;
import de.hsaa.shop.order.ShopOrder;
import de.hsaa.shop.order.ShopOrderRepository;
import de.hsaa.shop.product.Product;
import de.hsaa.shop.product.ProductRepository;
import de.hsaa.shop.returns.ReturnDtos.ConditionRequest;
import de.hsaa.shop.returns.ReturnDtos.ReturnItemRequest;
import de.hsaa.shop.returns.ReturnDtos.ReturnLineResponse;
import de.hsaa.shop.returns.ReturnDtos.ReturnRequest;
import de.hsaa.shop.returns.ReturnDtos.ReturnResponse;
import de.hsaa.shop.returns.ReturnDtos.ReturnUpdateRequest;

/**
 * Retouren (Sprint 3, docs/anforderungen/0025, 0026). Kunden melden Ware aus zugestellten Bestellungen
 * zurück und sehen den Stand (F1). Admins prüfen die Ware, erstatten das Geld, und originalverschlossene Rückläufer
 * kommen als reduzierte Ware wieder in den Verkauf (F2, ADR-0010).
 */
@Service
@Transactional
public class ReturnService {

	/** Frist für Retouren ab Zustellung. */
	static final Duration RETURN_PERIOD = Duration.ofDays(30);
	/** So viel billiger wird originalverschlossene Retourenware angeboten. */
	static final int RETURNED_GOODS_DISCOUNT_PERCENT = 30;
	private static final int MAX_PAGE_SIZE = 50;

	private final ProductReturnRepository returns;
	private final ShopOrderRepository orders;
	private final ProductRepository products;
	private final CurrentUser currentUser;

	public ReturnService(ProductReturnRepository returns, ShopOrderRepository orders, ProductRepository products,
			CurrentUser currentUser) {
		this.returns = returns;
		this.orders = orders;
		this.products = products;
		this.currentUser = currentUser;
	}

	// ---- F1: Kunde meldet eine Retoure an ----

	public ReturnResponse create(ReturnRequest request) {
		AppUser customer = currentUser.require();
		ShopOrder order = orders.findById(request.orderId())
				.filter(o -> o.getCustomer().getId().equals(customer.getId()))
				.orElseThrow(() -> new NotFoundException("Diese Bestellung gibt es nicht."));
		if (order.getStatus() != OrderStatus.DELIVERED) {
			throw new ConflictException("Zurückschicken geht erst, wenn die Bestellung zugestellt ist.");
		}
		if (order.getUpdatedAt().plus(RETURN_PERIOD).isBefore(Instant.now())) {
			throw new ConflictException("Die Rückgabefrist von 30 Tagen ist abgelaufen.");
		}

		Map<Long, Integer> wanted = new LinkedHashMap<>();
		for (ReturnItemRequest item : request.items()) {
			wanted.merge(item.productId(), item.quantity(), Integer::sum);
		}
		Map<Long, Integer> stillReturnable = returnable(order);
		ProductReturn productReturn = new ProductReturn(order, customer, request.reason().trim());
		for (Map.Entry<Long, Integer> entry : wanted.entrySet()) {
			OrderLine line = order.getLines().stream()
					.filter(l -> l.getProductId().equals(entry.getKey()))
					.findFirst()
					.orElseThrow(() -> new InvalidFieldException("items", "Dieses Produkt ist nicht in der Bestellung."));
			int max = stillReturnable.getOrDefault(line.getProductId(), 0);
			if (entry.getValue() > max) {
				throw new ConflictException("Von „" + line.getProductName() + "“ kannst du höchstens " + max
						+ " Stück zurückschicken.");
			}
			productReturn.addLine(new ReturnLine(line.getProductId(), line.getProductName(), line.getUnitPrice(),
					entry.getValue()));
		}
		return toResponse(returns.save(productReturn), customer);
	}

	/** Bestellt minus schon zurückgeschickt (abgelehnte Retouren zählen nicht). */
	private Map<Long, Integer> returnable(ShopOrder order) {
		Map<Long, Integer> left = new HashMap<>();
		for (OrderLine line : order.getLines()) {
			left.merge(line.getProductId(), line.getQuantity(), Integer::sum);
		}
		for (ProductReturn earlier : returns.findByOrderIdOrderByCreatedAtAsc(order.getId())) {
			if (earlier.getStatus() == ReturnStatus.REJECTED) {
				continue;
			}
			for (ReturnLine line : earlier.getLines()) {
				left.merge(line.getProductId(), -line.getQuantity(), Integer::sum);
			}
		}
		return left;
	}

	// ---- Ansehen: Kunde eigene, Admin alle ----

	@Transactional(readOnly = true)
	public PageResponse<ReturnResponse> list(ReturnStatus status, int page, int size) {
		AppUser viewer = currentUser.require();
		Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
				Sort.by("createdAt").descending().and(Sort.by("id").descending()));
		Page<ProductReturn> result;
		if (viewer.getRole() == Role.ADMIN) {
			result = status == null ? returns.findAll(pageable) : returns.findByStatus(status, pageable);
		}
		else {
			result = returns.findByCustomerId(viewer.getId(), pageable);
		}
		return PageResponse.of(result, r -> toResponse(r, viewer));
	}

	/** Alle Retouren zu einer Bestellung, für die Bestellseite. */
	@Transactional(readOnly = true)
	public List<ReturnResponse> forOrder(Long orderId) {
		AppUser viewer = currentUser.require();
		return returns.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
				.filter(r -> viewer.getRole() == Role.ADMIN || r.getCustomer().getId().equals(viewer.getId()))
				.map(r -> toResponse(r, viewer))
				.toList();
	}

	@Transactional(readOnly = true)
	public ReturnResponse get(Long id) {
		AppUser viewer = currentUser.require();
		return toResponse(loadVisible(id, viewer), viewer);
	}

	// ---- F2: Admin bearbeitet ----

	public ReturnResponse update(Long id, ReturnUpdateRequest request) {
		AppUser viewer = currentUser.require();
		if (viewer.getRole() != Role.ADMIN) {
			throw new ForbiddenException("Retouren bearbeitet nur der Shop.");
		}
		ProductReturn productReturn = loadVisible(id, viewer);
		ReturnStatus current = productReturn.getStatus();
		ReturnStatus target = request.status();
		if (!current.canChangeTo(target)) {
			throw new ConflictException("Eine Retoure im Status „" + current.label() + "“ kann nicht auf „"
					+ target.label() + "“ gesetzt werden.");
		}
		if (target == ReturnStatus.RECEIVED) {
			applyConditions(productReturn, request.conditions());
			restockUnused(productReturn);
		}
		if (target == ReturnStatus.REFUNDED) {
			productReturn.setRefundAmount(refundFor(productReturn));
		}
		if (request.note() != null && !request.note().isBlank()) {
			productReturn.setNote(request.note().trim());
		}
		productReturn.setStatus(target);
		return toResponse(productReturn, viewer);
	}

	/** Beim Eingang braucht jede Position einen Zustand, sonst wissen wir nicht, was wieder verkauft wird. */
	private static void applyConditions(ProductReturn productReturn, List<ConditionRequest> conditions) {
		Map<Long, ConditionRequest> byProduct = new HashMap<>();
		if (conditions != null) {
			conditions.forEach(c -> byProduct.put(c.productId(), c));
		}
		for (ReturnLine line : productReturn.getLines()) {
			ConditionRequest condition = byProduct.get(line.getProductId());
			if (condition == null) {
				throw new InvalidFieldException("conditions",
						"Bitte für jedes Produkt angeben, ob es originalverschlossen oder geöffnet ist.");
			}
			line.setCondition(condition.condition());
		}
	}

	/**
	 * F2: Originalverschlossene Teile kommen als reduzierte Ware wieder in den Verkauf. Dafür gibt es pro Produkt
	 * (Name + Größe) ein eigenes Produkt "Retourenware" mit eigenem Bestand, dauerhaft 30 % günstiger.
	 * So bleibt die neue Ware zum vollen Preis getrennt (ADR-0010).
	 */
	private void restockUnused(ProductReturn productReturn) {
		LocalDate today = ShopTime.today();
		for (ReturnLine line : productReturn.getLines()) {
			if (line.getCondition() != ItemCondition.UNUSED) {
				continue;
			}
			Optional<Product> original = products.findById(line.getProductId());
			String name = original.map(Product::getName).orElse(line.getProductName());
			String size = original.map(Product::getSize).orElse(null);
			Product twin = (size == null
					? products.findFirstByReturnedGoodsTrueAndNameAndSizeIsNull(name)
					: products.findFirstByReturnedGoodsTrueAndNameAndSize(name, size))
					.orElseGet(() -> original.map(o -> newReturnedGoods(o, today)).orElse(null));
			if (twin == null) {
				continue; // Originalprodukt gelöscht und keine Kategorie bekannt: nicht wieder verkaufen.
			}
			twin.setStock(twin.getStock() + line.getQuantity());
		}
	}

	private Product newReturnedGoods(Product original, LocalDate today) {
		String description = "Originalverschlossener Rückläufer, Siegel geprüft. "
				+ (original.getDescription() == null ? "" : original.getDescription());
		Product twin = new Product(original.getName(), description.length() > 1000 ? description.substring(0, 1000)
				: description, original.getPrice(), 0, original.getImageUrl(), original.getCategory(),
				original.getSize(), original.getForm());
		twin.setReturnedGoods(true);
		BigDecimal reduced = original.getPrice()
				.multiply(BigDecimal.valueOf(100 - RETURNED_GOODS_DISCOUNT_PERCENT))
				.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
		twin.startSale(reduced, today, null);
		return products.save(twin);
	}

	/** Erstattet wird, was der Kunde bezahlt hat: Preise aus der Bestellung, abzüglich Rabattcode (G2). */
	static BigDecimal refundFor(ProductReturn productReturn) {
		BigDecimal sum = productReturn.getLines().stream()
				.map(ReturnLine::getLineTotal)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		Integer percent = productReturn.getOrder().getDiscountPercent();
		if (percent == null) {
			return sum.setScale(2, RoundingMode.HALF_UP);
		}
		return sum.multiply(BigDecimal.valueOf(100 - percent)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
	}

	/** Fremde Retouren sehen für Kunden aus wie nicht vorhanden (wie bei Bestellungen). */
	private ProductReturn loadVisible(Long id, AppUser viewer) {
		ProductReturn productReturn = returns.findById(id)
				.orElseThrow(() -> new NotFoundException("Diese Retoure gibt es nicht."));
		if (viewer.getRole() != Role.ADMIN && !productReturn.getCustomer().getId().equals(viewer.getId())) {
			throw new NotFoundException("Diese Retoure gibt es nicht.");
		}
		return productReturn;
	}

	private ReturnResponse toResponse(ProductReturn r, AppUser viewer) {
		List<ReturnLineResponse> lines = r.getLines().stream()
				.sorted(Comparator.comparing(ReturnLine::getProductName))
				.map(l -> new ReturnLineResponse(l.getProductId(), l.getProductName(), l.getUnitPrice(),
						l.getQuantity(), l.getLineTotal(), l.getCondition()))
				.toList();
		List<ReturnStatus> allowed = viewer.getRole() == Role.ADMIN ? r.getStatus().next() : List.of();
		return new ReturnResponse(r.getId(), r.getOrder().getId(), r.getCreatedAt(), r.getUpdatedAt(), r.getStatus(),
				r.getReason(), r.getNote(), r.getRefundAmount(), refundFor(r), r.getCustomer().getName(), lines,
				allowed);
	}
}

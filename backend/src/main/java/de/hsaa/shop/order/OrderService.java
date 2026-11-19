package de.hsaa.shop.order;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.hsaa.shop.account.AppUser;
import de.hsaa.shop.account.CurrentUser;
import de.hsaa.shop.account.Role;
import de.hsaa.shop.account.SavedAddress;
import de.hsaa.shop.account.SavedAddressRepository;
import de.hsaa.shop.common.ConflictException;
import de.hsaa.shop.common.ForbiddenException;
import de.hsaa.shop.common.InvalidFieldException;
import de.hsaa.shop.common.NotFoundException;
import de.hsaa.shop.common.PageResponse;
import de.hsaa.shop.common.ShopTime;
import de.hsaa.shop.discount.DiscountCode;
import de.hsaa.shop.discount.DiscountService;
import de.hsaa.shop.order.OrderDtos.CustomerInfo;
import de.hsaa.shop.order.OrderDtos.DeliveryAddressResponse;
import de.hsaa.shop.order.OrderDtos.OrderItemRequest;
import de.hsaa.shop.order.OrderDtos.OrderLineResponse;
import de.hsaa.shop.order.OrderDtos.OrderRequest;
import de.hsaa.shop.order.OrderDtos.OrderResponse;
import de.hsaa.shop.order.OrderDtos.StockProblem;
import de.hsaa.shop.product.Product;
import de.hsaa.shop.product.ProductRepository;

/**
 * Bestellen und Bestellungen verwalten (docs/anforderungen/0009–0011, Lieferadresse 0017).
 * Kunden sehen und stornieren nur ihre eigenen Bestellungen, Admins sehen alle und ändern den Status.
 */
@Service
@Transactional
public class OrderService {

	static final int MAX_QUANTITY = 99;
	private static final int MAX_PAGE_SIZE = 50;

	private final ShopOrderRepository orders;
	private final ProductRepository products;
	private final CurrentUser currentUser;
	private final SavedAddressRepository addresses;
	private final DiscountService discounts;

	public OrderService(ShopOrderRepository orders, ProductRepository products, CurrentUser currentUser,
			SavedAddressRepository addresses, DiscountService discounts) {
		this.discounts = discounts;
		this.orders = orders;
		this.products = products;
		this.currentUser = currentUser;
		this.addresses = addresses;
	}

	/**
	 * Bestellung aufgeben. Alles in einer Transaktion: Produkte sperren, Bestand prüfen,
	 * Bestand abziehen, Bestellung speichern. Schlägt etwas fehl, passiert gar nichts (B2, B3).
	 */
	public OrderResponse place(OrderRequest request) {
		AppUser customer = currentUser.require();
		DeliveryAddress address = deliveryAddressFor(request, customer);

		// Gleiches Produkt mehrfach im Warenkorb → Mengen zusammenzählen.
		Map<Long, Integer> wanted = new LinkedHashMap<>();
		for (OrderItemRequest item : request.items()) {
			wanted.merge(item.productId(), item.quantity(), Integer::sum);
		}
		if (wanted.values().stream().anyMatch(quantity -> quantity > MAX_QUANTITY)) {
			throw new InvalidFieldException("items", "Höchstens " + MAX_QUANTITY + " Stück pro Produkt.");
		}

		Map<Long, Product> found = products.findAllByIdForUpdate(wanted.keySet()).stream()
				.collect(Collectors.toMap(Product::getId, Function.identity()));

		List<StockProblem> problems = new ArrayList<>();
		wanted.forEach((productId, quantity) -> {
			Product product = found.get(productId);
			if (product == null) {
				problems.add(new StockProblem(productId, "Unbekanntes Produkt", quantity, 0));
			}
			else if (product.getStock() < quantity) {
				problems.add(new StockProblem(productId, product.getName(), quantity, product.getStock()));
			}
		});
		if (!problems.isEmpty()) {
			throw new OutOfStockException(problems);
		}

		// Rabattcode vor dem Abziehen des Bestands prüfen: ist er ungültig, ändert sich gar nichts (G2).
		DiscountCode code = request.discountCode() == null || request.discountCode().isBlank() ? null
				: discounts.redeem(request.discountCode());

		LocalDate today = ShopTime.today();
		ShopOrder order = new ShopOrder(customer, address);
		wanted.forEach((productId, quantity) -> {
			Product product = found.get(productId);
			product.setStock(product.getStock() - quantity);
			// Preis, den Kunden heute zahlen: im Sale der Sale-Preis (G1). Er wird in die Position kopiert (E3).
			order.addLine(new OrderLine(product.getId(), lineName(product), product.currentPrice(today), quantity));
		});
		if (code != null) {
			order.applyDiscount(code.getCode(), code.getPercent());
		}
		return toResponse(orders.save(order), customer);
	}

	/**
	 * Gespeicherte Adresse aus dem Konto (E2) oder direkt eingegebene Adresse; beides wird in die
	 * Bestellung kopiert (ADR-0009). Fremde Adress-IDs sehen aus wie nicht vorhanden.
	 */
	private DeliveryAddress deliveryAddressFor(OrderRequest request, AppUser customer) {
		if (request.addressId() != null) {
			SavedAddress saved = addresses.findByIdAndOwnerId(request.addressId(), customer.getId())
					.orElseThrow(() -> new InvalidFieldException("addressId", "Diese Adresse gibt es nicht (mehr)."));
			return new DeliveryAddress(saved.getName(), saved.getStreet(), saved.getPostalCode(), saved.getCity());
		}
		if (request.deliveryAddress() != null) {
			return request.deliveryAddress().toAddress();
		}
		throw new InvalidFieldException("deliveryAddress", "Bitte gib eine Lieferadresse an.");
	}

	/** Name mit Packungsgröße in die Position kopieren, z. B. "Whey Protein Schoko (1 kg)" (0018). */
	private static String lineName(Product product) {
		String name = product.getDisplayName();
		return name.length() <= 100 ? name : name.substring(0, 100);
	}

	/** Kunden: eigene Bestellungen. Admins: alle. Neueste zuerst, optional nach Status gefiltert. */
	@Transactional(readOnly = true)
	public PageResponse<OrderResponse> list(OrderStatus status, int page, int size) {
		AppUser viewer = currentUser.require();
		Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
				Sort.by("createdAt").descending().and(Sort.by("id").descending()));

		Page<ShopOrder> result;
		if (viewer.getRole() == Role.ADMIN) {
			result = status == null ? orders.findAll(pageable) : orders.findByStatus(status, pageable);
		}
		else {
			result = status == null
					? orders.findByCustomerId(viewer.getId(), pageable)
					: orders.findByCustomerIdAndStatus(viewer.getId(), status, pageable);
		}
		return PageResponse.of(result, order -> toResponse(order, viewer));
	}

	@Transactional(readOnly = true)
	public OrderResponse get(Long id) {
		AppUser viewer = currentUser.require();
		return toResponse(loadVisible(id, viewer), viewer);
	}

	/**
	 * Status ändern. Admins: jeder erlaubte Übergang. Kunden: nur eigene Bestellung stornieren,
	 * solange sie noch nicht in Bearbeitung ist. Beim Stornieren kommt der Bestand zurück.
	 */
	public OrderResponse changeStatus(Long id, OrderStatus target) {
		AppUser viewer = currentUser.require();
		ShopOrder order = loadVisible(id, viewer);
		OrderStatus current = order.getStatus();

		if (viewer.getRole() != Role.ADMIN) {
			if (target != OrderStatus.CANCELLED) {
				throw new ForbiddenException("Du kannst eine Bestellung nur stornieren.");
			}
			if (current != OrderStatus.RECEIVED) {
				throw new ConflictException("Diese Bestellung kann nicht mehr storniert werden (Status: „"
						+ current.label() + "“). Bitte wende dich an den Shop.");
			}
		}
		if (current == target) {
			return toResponse(order, viewer);
		}
		if (!current.canChangeTo(target)) {
			throw new ConflictException("Eine Bestellung im Status „" + current.label()
					+ "“ kann nicht auf „" + target.label() + "“ gesetzt werden.");
		}

		if (target == OrderStatus.CANCELLED) {
			restoreStock(order);
		}
		order.setStatus(target);
		return toResponse(order, viewer);
	}

	private void restoreStock(ShopOrder order) {
		Map<Long, Integer> quantities = order.getLines().stream()
				.collect(Collectors.toMap(OrderLine::getProductId, OrderLine::getQuantity, Integer::sum));
		// Gelöschte Produkte werden übersprungen.
		for (Product product : products.findAllByIdForUpdate(quantities.keySet())) {
			product.setStock(product.getStock() + quantities.get(product.getId()));
		}
	}

	/** Fremde Bestellungen sehen für Kunden aus wie nicht vorhanden (404), damit nichts verraten wird. */
	private ShopOrder loadVisible(Long id, AppUser viewer) {
		ShopOrder order = orders.findById(id)
				.orElseThrow(() -> new NotFoundException("Diese Bestellung gibt es nicht."));
		if (viewer.getRole() != Role.ADMIN && !order.getCustomer().getId().equals(viewer.getId())) {
			throw new NotFoundException("Diese Bestellung gibt es nicht.");
		}
		return order;
	}

	private OrderResponse toResponse(ShopOrder order, AppUser viewer) {
		List<OrderLineResponse> lines = order.getLines().stream()
				.sorted(Comparator.comparing(OrderLine::getProductName))
				.map(line -> new OrderLineResponse(line.getProductId(), line.getProductName(), line.getUnitPrice(),
						line.getQuantity(), line.getLineTotal()))
				.toList();
		int itemCount = order.getLines().stream().mapToInt(OrderLine::getQuantity).sum();
		AppUser customer = order.getCustomer();

		List<OrderStatus> allowed;
		if (viewer.getRole() == Role.ADMIN) {
			allowed = order.getStatus().next();
		}
		else {
			allowed = order.getStatus() == OrderStatus.RECEIVED ? List.of(OrderStatus.CANCELLED) : List.of();
		}

		return new OrderResponse(order.getId(), order.getCreatedAt(), order.getUpdatedAt(), order.getStatus(),
				order.getTotal(), itemCount, new CustomerInfo(customer.getId(), customer.getName(), customer.getEmail()),
				DeliveryAddressResponse.from(order.getDeliveryAddress()), lines, allowed, order.getSubtotal(),
				order.getDiscountCode(), order.getDiscountPercent(), order.getDiscountAmount());
	}
}

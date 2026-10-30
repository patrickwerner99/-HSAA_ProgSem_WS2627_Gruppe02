package de.hsaa.shop.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import de.hsaa.shop.account.AppUser;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * Eine Bestellung. Heißt ShopOrder, weil "order" in SQL ein reserviertes Wort ist.
 * Preise und Namen werden beim Bestellen in die Positionen kopiert: Ändert der Admin später
 * den Preis oder löscht ein Produkt, bleibt die Bestellung trotzdem unverändert.
 */
@Entity
@Table(name = "shop_order")
public class ShopOrder {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "customer_id", nullable = false)
	private AppUser customer;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OrderStatus status;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal total;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id")
	private List<OrderLine> lines = new ArrayList<>();

	protected ShopOrder() {
		// für JPA
	}

	public ShopOrder(AppUser customer) {
		this.customer = customer;
		this.status = OrderStatus.RECEIVED;
		this.total = BigDecimal.ZERO;
		this.createdAt = Instant.now();
		this.updatedAt = this.createdAt;
	}

	/** Position hinzufügen; die Gesamtsumme rechnet immer der Server (B3). */
	public void addLine(OrderLine line) {
		line.setOrder(this);
		lines.add(line);
		total = total.add(line.getLineTotal());
	}

	public Long getId() {
		return id;
	}

	public AppUser getCustomer() {
		return customer;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public void setStatus(OrderStatus status) {
		this.status = status;
		this.updatedAt = Instant.now();
	}

	public BigDecimal getTotal() {
		return total;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public List<OrderLine> getLines() {
		return lines;
	}
}

package de.hsaa.shop.returns;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import de.hsaa.shop.account.AppUser;
import de.hsaa.shop.order.ShopOrder;
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
 * Eine Retoure: welche Teile einer zugestellten Bestellung zurückgehen und wie weit die Bearbeitung ist
 * (Sprint 3, F1/F2, docs/anforderungen/0025, 0026). Name und Preis jeder Position werden aus der
 * Bestellung kopiert, damit die Erstattung genau dem entspricht, was der Kunde bezahlt hat.
 */
@Entity
@Table(name = "product_return")
public class ProductReturn {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", nullable = false)
	private ShopOrder order;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "customer_id", nullable = false)
	private AppUser customer;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ReturnStatus status;

	@Column(nullable = false, length = 500)
	private String reason;

	/** Erstatteter Betrag, erst ab REFUNDED gesetzt. Berücksichtigt einen Rabattcode der Bestellung. */
	@Column(precision = 10, scale = 2)
	private BigDecimal refundAmount;

	/** Notiz des Admins, z. B. warum abgelehnt. Kunden sehen sie. */
	@Column(length = 500)
	private String note;

	@Column(nullable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	@OneToMany(mappedBy = "productReturn", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id")
	private List<ReturnLine> lines = new ArrayList<>();

	protected ProductReturn() {
		// für JPA
	}

	public ProductReturn(ShopOrder order, AppUser customer, String reason) {
		this.order = order;
		this.customer = customer;
		this.reason = reason;
		this.status = ReturnStatus.REQUESTED;
		this.createdAt = Instant.now();
		this.updatedAt = createdAt;
	}

	void addLine(ReturnLine line) {
		line.setProductReturn(this);
		lines.add(line);
	}

	void setStatus(ReturnStatus status) {
		this.status = status;
		this.updatedAt = Instant.now();
	}

	void setRefundAmount(BigDecimal refundAmount) {
		this.refundAmount = refundAmount;
	}

	void setNote(String note) {
		this.note = note;
	}

	public Long getId() {
		return id;
	}

	public ShopOrder getOrder() {
		return order;
	}

	public AppUser getCustomer() {
		return customer;
	}

	public ReturnStatus getStatus() {
		return status;
	}

	public String getReason() {
		return reason;
	}

	public BigDecimal getRefundAmount() {
		return refundAmount;
	}

	public String getNote() {
		return note;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public List<ReturnLine> getLines() {
		return lines;
	}
}

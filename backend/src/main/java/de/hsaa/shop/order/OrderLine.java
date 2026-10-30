package de.hsaa.shop.order;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Eine Position einer Bestellung. {@code productId} ist bewusst kein Fremdschlüssel:
 * Produkte dürfen gelöscht werden, alte Bestellungen bleiben trotzdem vollständig.
 */
@Entity
@Table(name = "order_line")
public class OrderLine {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", nullable = false)
	private ShopOrder order;

	@Column(nullable = false)
	private Long productId;

	@Column(nullable = false, length = 100)
	private String productName;

	/** Preis zum Zeitpunkt der Bestellung, kommt aus der Datenbank, nie aus dem Browser. */
	@Column(nullable = false, precision = 6, scale = 2)
	private BigDecimal unitPrice;

	@Column(nullable = false)
	private int quantity;

	protected OrderLine() {
		// für JPA
	}

	public OrderLine(Long productId, String productName, BigDecimal unitPrice, int quantity) {
		this.productId = productId;
		this.productName = productName;
		this.unitPrice = unitPrice;
		this.quantity = quantity;
	}

	void setOrder(ShopOrder order) {
		this.order = order;
	}

	public BigDecimal getLineTotal() {
		return unitPrice.multiply(BigDecimal.valueOf(quantity));
	}

	public Long getId() {
		return id;
	}

	public Long getProductId() {
		return productId;
	}

	public String getProductName() {
		return productName;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public int getQuantity() {
		return quantity;
	}
}

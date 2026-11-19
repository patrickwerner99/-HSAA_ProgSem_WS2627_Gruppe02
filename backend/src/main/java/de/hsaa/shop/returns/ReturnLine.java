package de.hsaa.shop.returns;

import java.math.BigDecimal;

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
import jakarta.persistence.Table;

/** Eine Position einer Retoure: welches Produkt, wie viele, und (nach Prüfung) in welchem Zustand. */
@Entity
@Table(name = "return_line")
public class ReturnLine {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "return_id", nullable = false)
	private ProductReturn productReturn;

	@Column(nullable = false)
	private Long productId;

	@Column(nullable = false, length = 100)
	private String productName;

	/** Preis aus der Bestellung, nicht der heutige Preis. */
	@Column(nullable = false, precision = 6, scale = 2)
	private BigDecimal unitPrice;

	@Column(nullable = false)
	private int quantity;

	/** null, bis der Admin die Ware geprüft hat. */
	@Enumerated(EnumType.STRING)
	@Column(name = "item_condition", length = 10)
	private ItemCondition condition;

	protected ReturnLine() {
		// für JPA
	}

	ReturnLine(Long productId, String productName, BigDecimal unitPrice, int quantity) {
		this.productId = productId;
		this.productName = productName;
		this.unitPrice = unitPrice;
		this.quantity = quantity;
	}

	void setProductReturn(ProductReturn productReturn) {
		this.productReturn = productReturn;
	}

	void setCondition(ItemCondition condition) {
		this.condition = condition;
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

	public ItemCondition getCondition() {
		return condition;
	}
}

package de.hsaa.shop.product;

import java.math.BigDecimal;

import de.hsaa.shop.category.Category;
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
 * Ein Produkt im Sortiment. In Sprint 1 ist jede Sorte ein eigenes Produkt
 * (z. B. "Whey Protein Schoko"), siehe docs/anforderungen/0004.
 */
@Entity
@Table(name = "product")
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(length = 1000)
	private String description;

	/** Preis in Euro. BigDecimal statt double, damit 34,90 nicht zu 34,899999 wird. */
	@Column(nullable = false, precision = 6, scale = 2)
	private BigDecimal price;

	/** Genauer Lagerbestand. Kunden sehen davon nur den {@link StockStatus}. */
	@Column(nullable = false)
	private int stock;

	@Column(length = 500)
	private String imageUrl;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	protected Product() {
		// für JPA
	}

	public Product(String name, String description, BigDecimal price, int stock, String imageUrl,
			Category category) {
		this.name = name;
		this.description = description;
		this.price = price;
		this.stock = stock;
		this.imageUrl = imageUrl;
		this.category = category;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public int getStock() {
		return stock;
	}

	public void setStock(int stock) {
		this.stock = stock;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}
}

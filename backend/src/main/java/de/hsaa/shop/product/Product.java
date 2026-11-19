package de.hsaa.shop.product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import de.hsaa.shop.category.Category;
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

/**
 * Ein Produkt im Sortiment. In Sprint 1 ist jede Sorte ein eigenes Produkt
 * (z. B. "Whey Protein Schoko"), siehe docs/anforderungen/0004. Seit Sprint 3 ist auch jede
 * Packungsgröße ein eigenes Produkt (z. B. "Whey Protein Schoko", 1 kg und 2,5 kg), weil Preis und
 * Bestand je Größe verschieden sind (docs/anforderungen/0018).
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

	/** Packungs- bzw. Portionsgröße als Text, z. B. "1 kg", "330 ml", "12 × 45 g" (0018). Optional. */
	@Column(length = 30)
	private String size;

	/**
	 * Darreichungsform (0019). In der Datenbank darf sie leer sein, damit ältere Produkte aus
	 * Sprint 2 beim Start nicht stören; neue und geänderte Produkte haben immer eine.
	 */
	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private ProductForm form;

	/**
	 * Sale (Sprint 3, G1, docs/anforderungen/0023): reduzierter Preis von {@code saleFrom} bis einschließlich
	 * {@code saleUntil}. Außerhalb des Zeitraums gilt wieder {@link #price}. {@code saleUntil = null} heißt
	 * "solange Vorrat reicht" und wird nur für Retourenware benutzt (F2), nie über die Admin-API.
	 */
	@Column(precision = 6, scale = 2)
	private BigDecimal salePrice;

	private LocalDate saleFrom;

	private LocalDate saleUntil;

	/**
	 * Barcode (EAN), optional. Darüber holt der Shop Nährwerte aus Open Food Facts (Sprint 3, H1,
	 * docs/anforderungen/0027, ADR-0011).
	 */
	@Column(length = 14)
	private String ean;

	/** Rückläufer aus einer Retoure, wieder im Verkauf als reduzierte Ware (F2, docs/anforderungen/0026). */
	@Column(nullable = false, columnDefinition = "boolean default false")
	private boolean returnedGoods;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	protected Product() {
		// für JPA
	}

	public Product(String name, String description, BigDecimal price, int stock, String imageUrl,
			Category category) {
		this(name, description, price, stock, imageUrl, category, null, null);
	}

	public Product(String name, String description, BigDecimal price, int stock, String imageUrl,
			Category category, String size, ProductForm form) {
		this.name = name;
		this.description = description;
		this.price = price;
		this.stock = stock;
		this.imageUrl = imageUrl;
		this.category = category;
		this.size = size;
		this.form = form;
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

	public String getSize() {
		return size;
	}

	public void setSize(String size) {
		this.size = size;
	}

	public ProductForm getForm() {
		return form;
	}

	public void setForm(ProductForm form) {
		this.form = form;
	}

	/** Name mit Größe, z. B. "Whey Protein Schoko (1 kg)". So steht es auch in der Bestellung. */
	public String getDisplayName() {
		return size == null ? name : name + " (" + size + ")";
	}

	// ---- Sale (G1) ----

	public boolean isOnSale(LocalDate today) {
		return salePrice != null && saleFrom != null && !today.isBefore(saleFrom)
				&& (saleUntil == null || !today.isAfter(saleUntil));
	}

	/** Der Preis, den Kunden heute zahlen: im Sale der Sale-Preis, sonst der normale Preis. */
	public BigDecimal currentPrice(LocalDate today) {
		return isOnSale(today) ? salePrice : price;
	}

	/** Ersparnis in ganzen Prozent, z. B. 20 bei 39,90 € statt 49,90 €. */
	public int savingPercent(LocalDate today) {
		if (!isOnSale(today) || price.signum() == 0) {
			return 0;
		}
		return price.subtract(salePrice).multiply(BigDecimal.valueOf(100))
				.divide(price, 0, RoundingMode.HALF_UP).intValue();
	}

	public void startSale(BigDecimal salePrice, LocalDate from, LocalDate until) {
		this.salePrice = salePrice;
		this.saleFrom = from;
		this.saleUntil = until;
	}

	public void endSale() {
		this.salePrice = null;
		this.saleFrom = null;
		this.saleUntil = null;
	}

	public BigDecimal getSalePrice() {
		return salePrice;
	}

	public LocalDate getSaleFrom() {
		return saleFrom;
	}

	public LocalDate getSaleUntil() {
		return saleUntil;
	}

	public boolean isReturnedGoods() {
		return returnedGoods;
	}

	public void setReturnedGoods(boolean returnedGoods) {
		this.returnedGoods = returnedGoods;
	}

	public String getEan() {
		return ean;
	}

	public void setEan(String ean) {
		this.ean = ean;
	}
}

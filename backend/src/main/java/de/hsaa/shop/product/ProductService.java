package de.hsaa.shop.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.hsaa.shop.category.Category;
import de.hsaa.shop.category.CategoryRepository;
import de.hsaa.shop.common.InvalidFieldException;
import de.hsaa.shop.common.NotFoundException;
import de.hsaa.shop.common.ShopTime;
import de.hsaa.shop.common.PageResponse;

/** Geschäftslogik für Produkte: Suche mit Nachladen, Anlegen, Ändern (ganz oder einzeln), Löschen. */
@Service
@Transactional
public class ProductService {

	/** Eine Ladung beim endlosen Scrollen (ADR-0002). */
	public static final int DEFAULT_PAGE_SIZE = 12;
	static final int MAX_PAGE_SIZE = 50;

	private final ProductRepository products;  //products= das Repository für Produkte
	private final CategoryRepository categories;

	public ProductService(ProductRepository products, CategoryRepository categories) {
		this.products = products;
		this.categories = categories;
	}

	/**
	 * Suchen, filtern und sortieren (docs/anforderungen/0001, 0012).
	 *
	 * @param form Produktart wie {@code RIEGEL} oder leer für alle (0019)
	 * @param sort {@code name} (Standard), {@code price-asc}, {@code price-desc} oder {@code newest}
	 */
	@Transactional(readOnly = true)
	public PageResponse<ProductResponse> search(String query, Long categoryId, String form, boolean inStockOnly,
			boolean saleOnly, String sort, int page, int size) {
		String text = query == null ? "" : query.trim();
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		Pageable pageable = PageRequest.of(safePage, safeSize, toSort(sort));

		Page<Product> result = products.search(text, categoryId, ProductForm.fromParam(form), inStockOnly, saleOnly,
				ShopTime.today(), pageable);
		return PageResponse.of(result, ProductResponse::from);
	}

	private static Sort toSort(String sort) {
		if (sort == null || sort.isBlank() || sort.equals("name")) {
			return Sort.by("name").ascending().and(Sort.by("id"));
		}
		return switch (sort) {
			// Sortiert nach dem Preis, den Kunden heute zahlen (im Sale der Sale-Preis, G1).
			case "price-asc" -> JpaSort.unsafe(Sort.Direction.ASC, ProductRepository.CURRENT_PRICE)
					.and(Sort.by("name")).and(Sort.by("id"));
			case "price-desc" -> JpaSort.unsafe(Sort.Direction.DESC, ProductRepository.CURRENT_PRICE)
					.and(Sort.by("name")).and(Sort.by("id"));
			// Größte Ersparnis zuerst, für den Sale-Bereich.
			case "saving" -> JpaSort.unsafe(Sort.Direction.DESC, "(p.price - " + ProductRepository.CURRENT_PRICE + ")")
					.and(Sort.by("name")).and(Sort.by("id"));
			// Neuere Produkte haben eine höhere ID.
			case "newest" -> Sort.by("id").descending();
			default -> throw new InvalidFieldException("sort",
					"Unbekannte Sortierung. Erlaubt: name, price-asc, price-desc, newest, saving.");
		};
	}

	/** Setzt die Bild-Adresse nach einem Upload und liefert die alte zurück (zum Aufräumen). */
	public String replaceImage(Long id, String imageUrl) {
		Product product = load(id);
		String old = product.getImageUrl();
		product.setImageUrl(imageUrl);
		return old;
	}



	@Transactional(readOnly = true)
	public ProductResponse findById(Long id) {
		return ProductResponse.from(load(id));
	}

	public ProductResponse create(ProductRequest request) {
		Product product = new Product(
				request.name().trim(),
				emptyToNull(request.description()),
				request.price(),
				request.stock().intValueExact(),
				emptyToNull(request.imageUrl()),
				loadCategory(request.categoryId()),
				emptyToNull(request.size()),
				request.form());
		product.setEan(emptyToNull(request.ean()));
		return ProductResponse.from(products.save(product));
	}

	/** Ganzes Produkt ändern: alle Felder werden überschrieben. */
	public ProductResponse replace(Long id, ProductRequest request) {
		Product product = load(id);
		product.setName(request.name().trim());
		product.setDescription(emptyToNull(request.description()));
		product.setPrice(request.price());
		product.setStock(request.stock().intValueExact());
		product.setImageUrl(emptyToNull(request.imageUrl()));
		product.setCategory(loadCategory(request.categoryId()));
		product.setSize(emptyToNull(request.size()));
		product.setForm(request.form());
		product.setEan(emptyToNull(request.ean()));
		return ProductResponse.from(product);
	}

	/** Einzelne Felder ändern: nur was mitgeschickt wurde, wird überschrieben. */
	public ProductResponse patch(Long id, ProductPatch patch) {
		Product product = load(id);
		if (patch.name() != null) {
			product.setName(patch.name().trim());
		}
		if (patch.description() != null) {
			product.setDescription(emptyToNull(patch.description()));
		}
		if (patch.price() != null) {
			product.setPrice(patch.price());
		}
		if (patch.stock() != null) {
			product.setStock(patch.stock().intValueExact());
		}
		if (patch.imageUrl() != null) {
			product.setImageUrl(emptyToNull(patch.imageUrl()));
		}
		if (patch.categoryId() != null) {
			product.setCategory(loadCategory(patch.categoryId()));
		}
		if (patch.size() != null) {
			product.setSize(emptyToNull(patch.size()));
		}
		if (patch.form() != null) {
			product.setForm(patch.form());
		}
		if (patch.ean() != null) {
			product.setEan(emptyToNull(patch.ean()));
		}
		return ProductResponse.from(product);
	}

	// ---- Sale (G1) ----

	public ProductResponse startSale(Long id, SaleRequest request) {
		Product product = load(id);
		if (request.salePrice().compareTo(product.getPrice()) >= 0) {
			throw new InvalidFieldException("salePrice",
					"Der Sale-Preis muss unter dem normalen Preis (" + product.getPrice() + " €) liegen.");
		}
		if (request.until().isBefore(request.from())) {
			throw new InvalidFieldException("until", "Das Ende darf nicht vor dem Beginn liegen.");
		}
		if (request.until().isBefore(ShopTime.today())) {
			throw new InvalidFieldException("until", "Das Ende darf nicht in der Vergangenheit liegen.");
		}
		product.startSale(request.salePrice(), request.from(), request.until());
		return ProductResponse.from(product);
	}

	public ProductResponse endSale(Long id) {
		Product product = load(id);
		product.endSale();
		return ProductResponse.from(product);
	}

	public void delete(Long id) {
		products.delete(load(id));
	}

	/** Für die Nährwerte (H1): Barcode des Produkts, 404 wenn es das Produkt nicht gibt. */
	@Transactional(readOnly = true)
	public String eanOf(Long id) {
		return load(id).getEan();
	}

	private Product load(Long id) {
		return products.findById(id) //"Repository, such mir das Produkte mit dieser ID"
				.orElseThrow(() -> new NotFoundException("Dieses Produkt gibt es nicht (mehr)."));
	}

	private Category loadCategory(Long categoryId) {
		return categories.findById(categoryId)
				.orElseThrow(() -> new InvalidFieldException("categoryId", "Diese Kategorie gibt es nicht (mehr)."));
	}

	private static String emptyToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}

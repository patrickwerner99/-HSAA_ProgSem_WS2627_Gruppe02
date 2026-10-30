package de.hsaa.shop.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.hsaa.shop.category.Category;
import de.hsaa.shop.category.CategoryRepository;
import de.hsaa.shop.common.InvalidFieldException;
import de.hsaa.shop.common.NotFoundException;
import de.hsaa.shop.common.PageResponse;

/** Geschäftslogik für Produkte: Suche mit Nachladen, Anlegen, Ändern (ganz oder einzeln), Löschen. */
@Service
@Transactional
public class ProductService {

	/** Eine Ladung beim endlosen Scrollen (ADR-0002). */
	public static final int DEFAULT_PAGE_SIZE = 12;
	static final int MAX_PAGE_SIZE = 50;

	private final ProductRepository products;
	private final CategoryRepository categories;

	public ProductService(ProductRepository products, CategoryRepository categories) {
		this.products = products;
		this.categories = categories;
	}

	/**
	 * Suchen, filtern und sortieren (docs/anforderungen/0001, 0012).
	 *
	 * @param sort {@code name} (Standard), {@code price-asc}, {@code price-desc} oder {@code newest}
	 */
	@Transactional(readOnly = true)
	public PageResponse<ProductResponse> search(String query, Long categoryId, boolean inStockOnly, String sort,
			int page, int size) {
		String text = query == null ? "" : query.trim();
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		Pageable pageable = PageRequest.of(safePage, safeSize, toSort(sort));

		Page<Product> result = products.search(text, categoryId, inStockOnly, pageable);
		return PageResponse.of(result, ProductResponse::from);
	}

	private static Sort toSort(String sort) {
		if (sort == null || sort.isBlank() || sort.equals("name")) {
			return Sort.by("name").ascending().and(Sort.by("id"));
		}
		return switch (sort) {
			case "price-asc" -> Sort.by("price").ascending().and(Sort.by("name")).and(Sort.by("id"));
			case "price-desc" -> Sort.by("price").descending().and(Sort.by("name")).and(Sort.by("id"));
			// Neuere Produkte haben eine höhere ID.
			case "newest" -> Sort.by("id").descending();
			default -> throw new InvalidFieldException("sort",
					"Unbekannte Sortierung. Erlaubt: name, price-asc, price-desc, newest.");
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
				loadCategory(request.categoryId()));
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
		return ProductResponse.from(product);
	}

	public void delete(Long id) {
		products.delete(load(id));
	}

	private Product load(Long id) {
		return products.findById(id)
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

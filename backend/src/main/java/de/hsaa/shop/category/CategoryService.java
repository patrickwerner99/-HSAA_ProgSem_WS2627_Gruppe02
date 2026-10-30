package de.hsaa.shop.category;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.hsaa.shop.common.ConflictException;
import de.hsaa.shop.common.NotFoundException;
import de.hsaa.shop.product.ProductRepository;

/** Geschäftsregeln für Kategorien: eindeutige Namen, Löschen nur wenn leer. */
@Service
@Transactional
public class CategoryService {

	private final CategoryRepository categories;
	private final ProductRepository products;

	public CategoryService(CategoryRepository categories, ProductRepository products) {
		this.categories = categories;
		this.products = products;
	}

	@Transactional(readOnly = true)
	public List<CategoryResponse> findAll() {
		return categories.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public CategoryResponse findById(Long id) {
		return toResponse(load(id));
	}

	public CategoryResponse create(CategoryRequest request) {
		String name = request.name().trim();
		if (categories.existsByNameIgnoreCase(name)) {
			throw new ConflictException("Diese Kategorie gibt es schon.");
		}
		return toResponse(categories.save(new Category(name)));
	}

	public CategoryResponse rename(Long id, CategoryRequest request) {
		Category category = load(id);
		String name = request.name().trim();
		if (categories.existsByNameIgnoreCaseAndIdNot(name, id)) {
			throw new ConflictException("Diese Kategorie gibt es schon.");
		}
		category.setName(name);
		return toResponse(category);
	}

	public void delete(Long id) {
		Category category = load(id);
		long count = products.countByCategoryId(id);
		if (count > 0) {
			String produkte = count == 1 ? "1 Produkt" : count + " Produkte";
			throw new ConflictException("Die Kategorie enthält noch " + produkte
					+ ". Bitte die Produkte zuerst verschieben oder löschen.");
		}
		categories.delete(category);
	}

	private Category load(Long id) {
		return categories.findById(id)
				.orElseThrow(() -> new NotFoundException("Diese Kategorie gibt es nicht (mehr)."));
	}

	private CategoryResponse toResponse(Category category) {
		return new CategoryResponse(category.getId(), category.getName(),
				products.countByCategoryId(category.getId()));
	}
}

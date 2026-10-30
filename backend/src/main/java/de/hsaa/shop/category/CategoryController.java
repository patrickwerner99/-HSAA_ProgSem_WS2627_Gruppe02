package de.hsaa.shop.category;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/** REST-Endpunkte für Kategorien unter /api/categories. */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

	private final CategoryService service;

	public CategoryController(CategoryService service) {
		this.service = service;
	}

	@GetMapping
	public List<CategoryResponse> list() {
		return service.findAll();
	}

	@GetMapping("/{id}")
	public CategoryResponse get(@PathVariable Long id) {
		return service.findById(id);
	}

	@PostMapping
	public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
		CategoryResponse created = service.create(request);
		return ResponseEntity.created(URI.create("/api/categories/" + created.id())).body(created);
	}

	@PutMapping("/{id}")
	public CategoryResponse rename(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
		return service.rename(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}
}

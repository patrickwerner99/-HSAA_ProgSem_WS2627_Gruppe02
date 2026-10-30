package de.hsaa.shop.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Datenbankzugriff für Produkte. Spring Data erzeugt die Abfragen aus den Methodennamen. */
public interface ProductRepository extends JpaRepository<Product, Long> {

	/** Suche im Namen, Groß-/Kleinschreibung egal. Ein leerer Suchtext findet alles. */
	Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

	Page<Product> findByCategoryIdAndNameContainingIgnoreCase(Long categoryId, String name, Pageable pageable);

	long countByCategoryId(Long categoryId);
}

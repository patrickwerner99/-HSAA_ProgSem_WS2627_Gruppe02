package de.hsaa.shop.product;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

/** Datenbankzugriff für Produkte. */
public interface ProductRepository extends JpaRepository<Product, Long> {

	/**
	 * Suche im Namen (Groß-/Kleinschreibung egal), optional nur eine Kategorie und nur
	 * verfügbare Produkte. Die Sortierung kommt über {@link Pageable}.
	 */
	@Query("""
			select p from Product p
			where lower(p.name) like lower(concat('%', :q, '%'))
			  and (:categoryId is null or p.category.id = :categoryId)
			  and (:inStockOnly = false or p.stock > 0)
			""")
	Page<Product> search(@Param("q") String q, @Param("categoryId") Long categoryId,
			@Param("inStockOnly") boolean inStockOnly, Pageable pageable);

	long countByCategoryId(Long categoryId);

	/**
	 * Lädt die Produkte einer Bestellung und sperrt sie bis zum Ende der Transaktion
	 * (SELECT ... FOR UPDATE). So können zwei gleichzeitige Bestellungen nicht beide das
	 * letzte Stück kaufen (B2, ADR-0006).
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Product p where p.id in :ids")
	List<Product> findAllByIdForUpdate(@Param("ids") Collection<Long> ids);
}

package de.hsaa.shop.product;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
	 * Suche im Namen (Groß-/Kleinschreibung egal), optional nur eine Kategorie, nur eine
	 * Produktart (0019), nur verfügbare Produkte und nur Produkte im Sale (G1). Die Sortierung kommt über {@link Pageable}.
	 */
	@Query("""
			select p from Product p
			where lower(p.name) like lower(concat('%', :q, '%'))
			  and (:categoryId is null or p.category.id = :categoryId)
			  and (:form is null or p.form = :form)
			  and (:inStockOnly = false or p.stock > 0)
			  and (:saleOnly = false or (p.salePrice is not null and p.saleFrom <= :today
			       and (p.saleUntil is null or p.saleUntil >= :today)))
			""")
	Page<Product> search(@Param("q") String q, @Param("categoryId") Long categoryId,
			@Param("form") ProductForm form, @Param("inStockOnly") boolean inStockOnly,
			@Param("saleOnly") boolean saleOnly, @Param("today") LocalDate today, Pageable pageable);

	/**
	 * Sortierausdruck "Preis, den Kunden heute zahlen" (im Sale der Sale-Preis). Nutzt den Parameter
	 * {@code :today} aus {@link #search}, damit die Sortierung zum angezeigten Preis passt (G1).
	 */
	String CURRENT_PRICE = "(case when p.salePrice is not null and p.saleFrom <= :today"
			+ " and (p.saleUntil is null or p.saleUntil >= :today) then p.salePrice else p.price end)";

	long countByCategoryId(Long categoryId);

	/**
	 * Lädt die Produkte einer Bestellung und sperrt sie bis zum Ende der Transaktion
	 * (SELECT ... FOR UPDATE). So können zwei gleichzeitige Bestellungen nicht beide das
	 * letzte Stück kaufen (B2, ADR-0006).
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Product p where p.id in :ids")
	List<Product> findAllByIdForUpdate(@Param("ids") Collection<Long> ids);

	/** Retourenware zu einem Produkt (F2): gleicher Name und gleiche Größe, als Rückläufer markiert. */
	Optional<Product> findFirstByReturnedGoodsTrueAndNameAndSize(String name, String size);

	Optional<Product> findFirstByReturnedGoodsTrueAndNameAndSizeIsNull(String name);
}

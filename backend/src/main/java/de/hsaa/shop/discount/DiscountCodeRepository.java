package de.hsaa.shop.discount;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface DiscountCodeRepository extends JpaRepository<DiscountCode, Long> {

	Optional<DiscountCode> findByCode(String code);

	boolean existsByCode(String code);

	List<DiscountCode> findAllByOrderByCreatedAtDesc();

	/** Beim Bestellen sperren, damit ein Code mit "höchstens N Mal" nicht gleichzeitig überzogen wird (wie ADR-0006). */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select d from DiscountCode d where d.code = :code")
	Optional<DiscountCode> findByCodeForUpdate(@Param("code") String code);
}

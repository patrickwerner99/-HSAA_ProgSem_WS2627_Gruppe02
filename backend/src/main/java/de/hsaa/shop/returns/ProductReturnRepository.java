package de.hsaa.shop.returns;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductReturnRepository extends JpaRepository<ProductReturn, Long> {

	Page<ProductReturn> findByCustomerId(Long customerId, Pageable pageable);

	Page<ProductReturn> findByStatus(ReturnStatus status, Pageable pageable);

	List<ProductReturn> findByOrderIdOrderByCreatedAtAsc(Long orderId);
}

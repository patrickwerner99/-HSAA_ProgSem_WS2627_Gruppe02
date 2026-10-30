package de.hsaa.shop.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {

	Page<ShopOrder> findByCustomerId(Long customerId, Pageable pageable);

	Page<ShopOrder> findByCustomerIdAndStatus(Long customerId, OrderStatus status, Pageable pageable);

	Page<ShopOrder> findByStatus(OrderStatus status, Pageable pageable);
}

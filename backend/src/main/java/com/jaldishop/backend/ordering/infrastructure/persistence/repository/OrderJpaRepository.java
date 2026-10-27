package com.jaldishop.backend.ordering.infrastructure.persistence.repository;

import com.jaldishop.backend.ordering.infrastructure.persistence.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {

    Optional<OrderEntity> findByPaymentId(UUID paymentId);

    Optional<OrderEntity> findByCapacityReservationId(UUID capacityReservationId);

    List<OrderEntity> findByUserIdOrderByConfirmedAtDesc(UUID userId);

    Optional<OrderEntity> findByOrderNumber(String orderNumber);

    @Query(value = "SELECT nextval('seq_order_number')", nativeQuery = true)
    long nextOrderNumber();

    @Query(value = """
        SELECT COUNT(oi.id) > 0
        FROM orders o
        JOIN order_items oi ON oi.order_id = o.id
        JOIN product_variants pv ON pv.id = oi.variant_id
        WHERE o.user_id = :userId
          AND o.status = 'COMPLETED'
          AND pv.product_id = :productId
    """, nativeQuery = true)
    boolean hasCompletedOrderWithProduct(@Param("userId") UUID userId, @Param("productId") UUID productId);

}

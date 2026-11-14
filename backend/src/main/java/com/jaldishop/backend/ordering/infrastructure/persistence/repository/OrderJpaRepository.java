package com.jaldishop.backend.ordering.infrastructure.persistence.repository;

import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.ordering.infrastructure.persistence.entity.OrderEntity;
import com.jaldishop.backend.ordering.infrastructure.persistence.projection.OrderStatusAggregationProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OrderEntity o WHERE o.id = :id")
    Optional<OrderEntity> findByIdForUpdate(@Param("id") UUID id);

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

    Page<OrderEntity> findByStoreIdOrderByConfirmedAtDescIdDesc(UUID storeId, Pageable pageable);

    Page<OrderEntity> findByStoreIdAndStatusOrderByConfirmedAtDescIdDesc(UUID storeId, OrderStatus status, Pageable pageable);

    Optional<OrderEntity> findByIdAndStoreId(UUID id, UUID storeId);

    Optional<OrderEntity> findByOrderNumberAndStoreId(String orderNumber, UUID storeId);

    @Query("""
        SELECT
            COUNT(o) AS total,
            COUNT(CASE WHEN o.status = com.jaldishop.backend.ordering.domain.enums.OrderStatus.CONFIRMED THEN 1 END) AS confirmed,
            COUNT(CASE WHEN o.status = com.jaldishop.backend.ordering.domain.enums.OrderStatus.IN_PREPARATION THEN 1 END) AS inPreparation,
            COUNT(CASE WHEN o.status = com.jaldishop.backend.ordering.domain.enums.OrderStatus.READY THEN 1 END) AS ready,
            COUNT(CASE WHEN o.status = com.jaldishop.backend.ordering.domain.enums.OrderStatus.OUT_FOR_DELIVERY THEN 1 END) AS outForDelivery,
            COUNT(CASE WHEN o.status = com.jaldishop.backend.ordering.domain.enums.OrderStatus.COMPLETED THEN 1 END) AS completed,
            COUNT(CASE WHEN o.status = com.jaldishop.backend.ordering.domain.enums.OrderStatus.CANCELLED THEN 1 END) AS cancelled
        FROM OrderEntity o
        WHERE o.storeId = :storeId
    """)
    OrderStatusAggregationProjection countOrdersByStoreId(@Param("storeId") UUID storeId);

    @Query("""
        SELECT
            COUNT(o) AS total,
            COUNT(CASE WHEN o.status = com.jaldishop.backend.ordering.domain.enums.OrderStatus.CONFIRMED THEN 1 END) AS confirmed,
            COUNT(CASE WHEN o.status = com.jaldishop.backend.ordering.domain.enums.OrderStatus.IN_PREPARATION THEN 1 END) AS inPreparation,
            COUNT(CASE WHEN o.status = com.jaldishop.backend.ordering.domain.enums.OrderStatus.READY THEN 1 END) AS ready,
            COUNT(CASE WHEN o.status = com.jaldishop.backend.ordering.domain.enums.OrderStatus.OUT_FOR_DELIVERY THEN 1 END) AS outForDelivery,
            COUNT(CASE WHEN o.status = com.jaldishop.backend.ordering.domain.enums.OrderStatus.COMPLETED THEN 1 END) AS completed,
            COUNT(CASE WHEN o.status = com.jaldishop.backend.ordering.domain.enums.OrderStatus.CANCELLED THEN 1 END) AS cancelled
        FROM OrderEntity o
        WHERE o.storeId = :storeId
          AND o.serviceDate = :serviceDate
    """)
    OrderStatusAggregationProjection countOrdersByStoreIdAndServiceDate(
            @Param("storeId") UUID storeId,
            @Param("serviceDate") LocalDate serviceDate
    );

}

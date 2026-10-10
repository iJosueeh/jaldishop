package com.jaldishop.backend.ordering.infrastructure.persistence.repository;

import com.jaldishop.backend.ordering.infrastructure.persistence.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID> {

    Optional<OrderEntity> findByPaymentId(UUID paymentId);

    Optional<OrderEntity> findByCapacityReservationId(UUID capacityReservationId);

    @Query(value = "SELECT nextval('seq_order_number')", nativeQuery = true)
    long nextOrderNumber();

}
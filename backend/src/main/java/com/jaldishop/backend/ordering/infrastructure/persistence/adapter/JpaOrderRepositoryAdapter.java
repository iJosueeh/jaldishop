package com.jaldishop.backend.ordering.infrastructure.persistence.adapter;

import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.ordering.infrastructure.persistence.entity.OrderEntity;
import com.jaldishop.backend.ordering.infrastructure.persistence.mapper.OrderPersistenceMapper;
import com.jaldishop.backend.ordering.infrastructure.persistence.repository.OrderJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaOrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpaRepository;
    private final OrderPersistenceMapper mapper;

    public JpaOrderRepositoryAdapter(OrderJpaRepository jpaRepository, OrderPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Order save(Order order) {
        OrderEntity entity = mapper.toEntity(order);
        OrderEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Order> findByIdForUpdate(UUID id) {
        return jpaRepository.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Order> findByPaymentId(UUID paymentId) {
        return jpaRepository.findByPaymentId(paymentId).map(mapper::toDomain);
    }

    @Override
    public Optional<Order> findByCapacityReservationId(UUID capacityReservationId) {
        return jpaRepository.findByCapacityReservationId(capacityReservationId).map(mapper::toDomain);
    }

    @Override
    public List<Order> findByUserId(UUID userId) {
        return jpaRepository.findByUserIdOrderByConfirmedAtDesc(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Order> findByOrderNumber(String orderNumber) {
        return jpaRepository.findByOrderNumber(orderNumber).map(mapper::toDomain);
    }

    @Override
    public long nextOrderNumber() {
        return jpaRepository.nextOrderNumber();
    }

    @Override
    public boolean hasCompletedOrderWithProduct(UUID userId, UUID productId) {
        if (userId == null || productId == null) {
            return false;
        }
        return jpaRepository.hasCompletedOrderWithProduct(userId, productId);
    }

    @Override
    public Page<Order> findByStoreId(UUID storeId, Pageable pageable) {
        return jpaRepository.findByStoreIdOrderByConfirmedAtDescIdDesc(storeId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<Order> findByStoreIdAndStatus(UUID storeId, OrderStatus status, Pageable pageable) {
        return jpaRepository.findByStoreIdAndStatusOrderByConfirmedAtDescIdDesc(storeId, status, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Order> findByIdAndStoreId(UUID id, UUID storeId) {
        return jpaRepository.findByIdAndStoreId(id, storeId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Order> findByOrderNumberAndStoreId(String orderNumber, UUID storeId) {
        return jpaRepository.findByOrderNumberAndStoreId(orderNumber, storeId)
                .map(mapper::toDomain);
    }
}

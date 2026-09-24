package com.jaldishop.backend.store.infrastructure.persistence.adapter;

import com.jaldishop.backend.store.domain.StoreCustomer;
import com.jaldishop.backend.store.domain.StoreCustomerRepository;
import com.jaldishop.backend.store.infrastructure.persistence.entity.StoreCustomerEntity;
import com.jaldishop.backend.store.infrastructure.persistence.entity.StoreCustomerId;
import com.jaldishop.backend.store.infrastructure.persistence.projection.StoreCustomerProjection;
import com.jaldishop.backend.store.infrastructure.persistence.repository.StoreCustomerJpaRepository;
import com.jaldishop.backend.store.web.dto.StoreCustomerResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class StoreCustomerRepositoryAdapter implements StoreCustomerRepository {

    private final StoreCustomerJpaRepository storeCustomerJpaRepository;

    public StoreCustomerRepositoryAdapter(StoreCustomerJpaRepository storeCustomerJpaRepository) {
        this.storeCustomerJpaRepository = storeCustomerJpaRepository;
    }

    @Override
    public void save(StoreCustomer storeCustomer) {
        StoreCustomerId id = new StoreCustomerId(storeCustomer.getStoreId(), storeCustomer.getUserId());
        StoreCustomerEntity entity = new StoreCustomerEntity(id, storeCustomer.getCreatedAt());
        storeCustomerJpaRepository.save(entity);
    }

    @Override
    public boolean existsByStoreIdAndUserId(UUID storeId, UUID userId) {
        return storeCustomerJpaRepository.existsByIdStoreIdAndIdUserId(storeId, userId);
    }

    @Override
    public List<StoreCustomerResponse> findCustomersByStoreId(UUID storeId, String query) {
        List<StoreCustomerProjection> projections =
                storeCustomerJpaRepository.findCustomersByStoreId(storeId, query != null ? query.trim() : null);

        return projections.stream()
                .map(p -> new StoreCustomerResponse(
                        p.getUserId(),
                        p.getFirstName(),
                        p.getLastName(),
                        p.getEmail(),
                        p.getPhone(),
                        p.getCustomerSince() != null ? p.getCustomerSince().toInstant() : null,
                        p.getOrdersCount() != null ? p.getOrdersCount() : 0L,
                        p.getTotalSpent() != null ? p.getTotalSpent() : BigDecimal.ZERO,
                        p.getLastOrderAt() != null ? p.getLastOrderAt().toInstant() : null
                ))
                .collect(Collectors.toList());
    }
}

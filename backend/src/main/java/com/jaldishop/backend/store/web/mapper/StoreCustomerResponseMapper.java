package com.jaldishop.backend.store.web.mapper;

import com.jaldishop.backend.store.domain.StoreCustomerSummary;
import com.jaldishop.backend.store.web.dto.StoreCustomerResponse;
import org.springframework.stereotype.Component;

/**
 * Mapeo exclusivo de la capa web: {@code StoreCustomerSummary} (dominio) a
 * {@code StoreCustomerResponse} (contrato REST), preservando los 9 campos.
 */
@Component
public class StoreCustomerResponseMapper {

    public StoreCustomerResponse toResponse(StoreCustomerSummary summary) {
        return new StoreCustomerResponse(
                summary.userId(),
                summary.firstName(),
                summary.lastName(),
                summary.email(),
                summary.phone(),
                summary.customerSince(),
                summary.ordersCount(),
                summary.totalSpentAmount(),
                summary.lastOrderAt()
        );
    }
}

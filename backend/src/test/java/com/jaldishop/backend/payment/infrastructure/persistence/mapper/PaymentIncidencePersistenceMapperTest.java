package com.jaldishop.backend.payment.infrastructure.persistence.mapper;

import com.jaldishop.backend.payment.domain.enums.IncidenceCategory;
import com.jaldishop.backend.payment.domain.enums.IncidenceProcessType;
import com.jaldishop.backend.payment.domain.enums.IncidenceStatus;
import com.jaldishop.backend.payment.domain.model.PaymentIncidence;
import com.jaldishop.backend.payment.infrastructure.persistence.entity.PaymentIncidenceEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PaymentIncidencePersistenceMapper — Pruebas unitarias")
class PaymentIncidencePersistenceMapperTest {

    private final PaymentIncidencePersistenceMapper mapper = new PaymentIncidencePersistenceMapper();

    @Test
    @DisplayName("Debe mapear de dominio a entidad y viceversa de forma bidireccional")
    void shouldMapBidirectionally() {
        UUID id = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Instant now = Instant.now();

        PaymentIncidence domain = new PaymentIncidence(
                id,
                paymentId,
                IncidenceProcessType.ORDER_CONFIRMATION,
                IncidenceCategory.COMMERCIAL,
                "INSUFFICIENT_STOCK",
                "Sin inventario",
                2,
                5,
                now.plusSeconds(300),
                IncidenceStatus.OPEN,
                now,
                now,
                null
        );

        PaymentIncidenceEntity entity = mapper.toEntity(domain);

        assertNotNull(entity);
        assertEquals(id, entity.getId());
        assertEquals(paymentId, entity.getPaymentId());
        assertEquals("ORDER_CONFIRMATION", entity.getProcessType());
        assertEquals("COMMERCIAL", entity.getCategory());
        assertEquals("INSUFFICIENT_STOCK", entity.getCode());
        assertEquals("Sin inventario", entity.getMessage());
        assertEquals(2, entity.getAttemptsCount());
        assertEquals(5, entity.getMaxAttempts());
        assertEquals("OPEN", entity.getStatus());

        PaymentIncidence convertedDomain = mapper.toDomain(entity);

        assertNotNull(convertedDomain);
        assertEquals(domain.getId(), convertedDomain.getId());
        assertEquals(domain.getPaymentId(), convertedDomain.getPaymentId());
        assertEquals(domain.getProcessType(), convertedDomain.getProcessType());
        assertEquals(domain.getCategory(), convertedDomain.getCategory());
        assertEquals(domain.getCode(), convertedDomain.getCode());
        assertEquals(domain.getStatus(), convertedDomain.getStatus());
    }
}

package com.jaldishop.backend.inventory.application.service;

import com.jaldishop.backend.inventory.domain.enums.InventoryReservationStatus;
import com.jaldishop.backend.inventory.domain.model.InventoryReservation;
import com.jaldishop.backend.inventory.domain.repository.InventoryReservationRepository;
import com.jaldishop.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.jaldishop.backend.inventory.infrastructure.persistence.repository.InventoryJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RestockInventoryService — Pruebas unitarias de restitución de stock")
class RestockInventoryServiceTest {

    @Mock
    private InventoryJpaRepository inventoryJpaRepository;

    @Mock
    private InventoryReservationRepository inventoryReservationRepository;

    private RestockInventoryService service;

    private UUID storeId;
    private UUID capacityReservationId;
    private UUID variantId1;
    private UUID variantId2;

    @BeforeEach
    void setUp() {
        service = new RestockInventoryService(inventoryJpaRepository, inventoryReservationRepository);
        storeId = UUID.randomUUID();
        capacityReservationId = UUID.randomUUID();
        variantId1 = UUID.randomUUID();
        variantId2 = UUID.randomUUID();
    }

    @Test
    @DisplayName("Restitución exitosa: incrementa existencias físicas y marca reservas en RELEASED")
    void shouldRestockInventoryAndMarkReservationsReleased() {
        InventoryReservation res1 = new InventoryReservation(
                UUID.randomUUID(), storeId, capacityReservationId, variantId1, 3,
                InventoryReservationStatus.COMMITTED, Instant.now().plusSeconds(600), Instant.now(), Instant.now()
        );
        InventoryReservation res2 = new InventoryReservation(
                UUID.randomUUID(), storeId, capacityReservationId, variantId2, 2,
                InventoryReservationStatus.COMMITTED, Instant.now().plusSeconds(600), Instant.now(), Instant.now()
        );

        when(inventoryReservationRepository.findByCapacityReservationId(capacityReservationId))
                .thenReturn(List.of(res1, res2));

        InventoryEntity inv1 = new InventoryEntity(variantId1, 10, 2, Instant.now());
        InventoryEntity inv2 = new InventoryEntity(variantId2, 5, 2, Instant.now());

        when(inventoryJpaRepository.findByVariantIdInForUpdate(any())).thenReturn(List.of(inv1, inv2));

        service.execute(storeId, capacityReservationId);

        // inv1: 10 + 3 = 13
        assertThat(inv1.getQuantity()).isEqualTo(13);
        // inv2: 5 + 2 = 7
        assertThat(inv2.getQuantity()).isEqualTo(7);

        verify(inventoryJpaRepository).save(inv1);
        verify(inventoryJpaRepository).save(inv2);

        assertThat(res1.getStatus()).isEqualTo(InventoryReservationStatus.RELEASED);
        assertThat(res2.getStatus()).isEqualTo(InventoryReservationStatus.RELEASED);
        verify(inventoryReservationRepository).save(res1);
        verify(inventoryReservationRepository).save(res2);
    }

    @Test
    @DisplayName("Idempotencia: si las reservas ya están en RELEASED, no realiza incrementos ni bloqueos")
    void shouldBeIdempotentWhenNoCommittedReservationsExist() {
        InventoryReservation res1 = new InventoryReservation(
                UUID.randomUUID(), storeId, capacityReservationId, variantId1, 3,
                InventoryReservationStatus.RELEASED, Instant.now().plusSeconds(600), Instant.now(), Instant.now()
        );

        when(inventoryReservationRepository.findByCapacityReservationId(capacityReservationId))
                .thenReturn(List.of(res1));

        service.execute(storeId, capacityReservationId);

        verify(inventoryJpaRepository, never()).findByVariantIdInForUpdate(any());
        verify(inventoryJpaRepository, never()).save(any());
        verify(inventoryReservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Idempotencia: si la capacidad es nula, no ejecuta acciones")
    void shouldDoNothingWhenCapacityReservationIdIsNull() {
        service.execute(storeId, null);

        verify(inventoryReservationRepository, never()).findByCapacityReservationId(any());
    }
}

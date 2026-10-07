package com.jaldishop.backend.inventory.application;

import com.jaldishop.backend.checkout.domain.CheckoutItemSnapshot;
import com.jaldishop.backend.inventory.domain.InventoryReservation;
import com.jaldishop.backend.inventory.domain.InventoryReservationRepository;
import com.jaldishop.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.jaldishop.backend.inventory.infrastructure.persistence.repository.InventoryJpaRepository;
import com.jaldishop.backend.shared.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReserveInventoryServiceTest {

    @Mock
    private InventoryJpaRepository inventoryJpaRepository;

    @Mock
    private InventoryReservationRepository inventoryReservationRepository;

    @InjectMocks
    private ReserveInventoryService reserveInventoryService;

    private UUID storeId;
    private UUID capacityReservationId;
    private Instant expiresAt;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        capacityReservationId = UUID.randomUUID();
        expiresAt = Instant.now().plusSeconds(600);
    }

    private CheckoutItemSnapshot createSnapshot(UUID variantId, int quantity, boolean tracksInventory) {
        return new CheckoutItemSnapshot(
                variantId,
                UUID.randomUUID(),
                "Producto Prueba",
                "Presentación",
                "SKU-1",
                "https://example.com/img.png",
                quantity,
                BigDecimal.TEN,
                "PEN",
                BigDecimal.valueOf(quantity * 10),
                tracksInventory
        );
    }

    @Test
    @DisplayName("Cuando ningún ítem rastrea inventario (tracksInventory=false), no adquiere locks ni crea reservas")
    void shouldReturnEmptyWhenNoItemsTrackInventory() {
        UUID var1 = UUID.randomUUID();
        var items = List.of(createSnapshot(var1, 2, false));

        List<InventoryReservation> result = reserveInventoryService.execute(storeId, capacityReservationId, expiresAt, items);

        assertThat(result).isEmpty();
        verifyNoInteractions(inventoryJpaRepository);
        verifyNoInteractions(inventoryReservationRepository);
    }

    @Test
    @DisplayName("Cuando tracksInventory=true y falta fila en inventories, arroja INVENTORY_NOT_CONFIGURED")
    void shouldThrowInventoryNotConfiguredWhenRowMissing() {
        UUID var1 = UUID.randomUUID();
        var items = List.of(createSnapshot(var1, 2, true));

        when(inventoryJpaRepository.findByVariantIdInForUpdate(List.of(var1))).thenReturn(List.of());

        assertThatThrownBy(() -> reserveInventoryService.execute(storeId, capacityReservationId, expiresAt, items))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> {
                    ConflictException ce = (ConflictException) ex;
                    assertThat(ce.getCode()).isEqualTo("INVENTORY_NOT_CONFIGURED");
                });

        verifyNoInteractions(inventoryReservationRepository);
    }

    @Test
    @DisplayName("Cuando el stock disponible es insuficiente, arroja INSUFFICIENT_STOCK")
    void shouldThrowInsufficientStockWhenAvailableLessThanRequested() {
        UUID var1 = UUID.randomUUID();
        var items = List.of(createSnapshot(var1, 3, true));

        InventoryEntity entity = new InventoryEntity(var1, 5, 1, Instant.now());
        when(inventoryJpaRepository.findByVariantIdInForUpdate(List.of(var1))).thenReturn(List.of(entity));
        // Ya hay 3 unidades reservadas activas: disponible = 5 - 3 = 2 < 3 solicitadas
        when(inventoryJpaRepository.sumActiveReservedQuantity(eq(storeId), eq(var1), any(Instant.class))).thenReturn(3);

        assertThatThrownBy(() -> reserveInventoryService.execute(storeId, capacityReservationId, expiresAt, items))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> {
                    ConflictException ce = (ConflictException) ex;
                    assertThat(ce.getCode()).isEqualTo("INSUFFICIENT_STOCK");
                });

        verifyNoInteractions(inventoryReservationRepository);
    }

    @Test
    @DisplayName("Agrupa cantidades repetidas de una misma variante y bloquea en orden canónico")
    void shouldGroupRepeatedVariantsAndLockCanonically() {
        UUID varB = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID varA = UUID.fromString("00000000-0000-0000-0000-000000000001");

        // Ítems desordenados y varA repetida (2 + 3 = 5 unidades)
        var items = List.of(
                createSnapshot(varB, 1, true),
                createSnapshot(varA, 2, true),
                createSnapshot(varA, 3, true)
        );

        InventoryEntity entA = new InventoryEntity(varA, 10, 2, Instant.now());
        InventoryEntity entB = new InventoryEntity(varB, 5, 1, Instant.now());

        // Debe solicitarse en orden canónico varA (menor) luego varB (mayor)
        when(inventoryJpaRepository.findByVariantIdInForUpdate(List.of(varA, varB)))
                .thenReturn(List.of(entA, entB));

        when(inventoryJpaRepository.sumActiveReservedQuantity(eq(storeId), eq(varA), any(Instant.class))).thenReturn(0);
        when(inventoryJpaRepository.sumActiveReservedQuantity(eq(storeId), eq(varB), any(Instant.class))).thenReturn(0);

        when(inventoryReservationRepository.save(any(InventoryReservation.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        List<InventoryReservation> result = reserveInventoryService.execute(storeId, capacityReservationId, expiresAt, items);

        assertThat(result).hasSize(2);

        ArgumentCaptor<InventoryReservation> captor = ArgumentCaptor.forClass(InventoryReservation.class);
        verify(inventoryReservationRepository, times(2)).save(captor.capture());

        List<InventoryReservation> saved = captor.getAllValues();
        InventoryReservation resA = saved.stream().filter(r -> r.getVariantId().equals(varA)).findFirst().orElseThrow();
        InventoryReservation resB = saved.stream().filter(r -> r.getVariantId().equals(varB)).findFirst().orElseThrow();

        assertThat(resA.getQuantity()).isEqualTo(5); // 2 + 3 agrupados
        assertThat(resB.getQuantity()).isEqualTo(1);
    }
}

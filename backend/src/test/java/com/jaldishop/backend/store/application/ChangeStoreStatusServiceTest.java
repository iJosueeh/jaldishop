package com.jaldishop.backend.store.application;

import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import com.jaldishop.backend.store.domain.StoreStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangeStoreStatusServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @InjectMocks
    private ChangeStoreStatusService changeStoreStatusService;

    private Store activeStore;
    private UUID storeId;
    private UUID merchantId;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        storeId = UUID.randomUUID();

        activeStore = Store.create(
                merchantId,
                "Bakery Demo",
                "bakery-demo",
                "Panaderia y pasteleria",
                "987654321",
                "Av. Principal 123",
                "Cerca a plaza",
                null,
                null,
                true,
                true,
                BigDecimal.valueOf(5),
                "PEN",
                null
        );
    }

    @Test
    @DisplayName("Debe suspender una tienda activa correctamente")
    void shouldSuspendActiveStore() {
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(activeStore));
        when(storeRepository.save(any(Store.class))).thenAnswer(inv -> inv.getArgument(0));

        Store response = changeStoreStatusService.execute(new ChangeStoreStatusCommand(storeId, StoreStatus.SUSPENDED));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(StoreStatus.SUSPENDED);
        verify(storeRepository).save(activeStore);
    }

    @Test
    @DisplayName("Debe lanzar excepción al suspender una tienda ya suspendida")
    void shouldThrowWhenStoreAlreadySuspended() {
        activeStore.suspend();
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(activeStore));

        assertThatThrownBy(() -> changeStoreStatusService.execute(new ChangeStoreStatusCommand(storeId, StoreStatus.SUSPENDED)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya se encuentra suspendida");
    }

    @Test
    @DisplayName("Debe reactivar una tienda suspendida correctamente")
    void shouldActivateSuspendedStore() {
        activeStore.suspend();
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(activeStore));
        when(storeRepository.save(any(Store.class))).thenAnswer(inv -> inv.getArgument(0));

        Store response = changeStoreStatusService.execute(new ChangeStoreStatusCommand(storeId, StoreStatus.ACTIVE));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(StoreStatus.ACTIVE);
        verify(storeRepository).save(activeStore);
    }

    @Test
    @DisplayName("Debe lanzar excepción si la tienda no existe")
    void shouldThrowWhenStoreNotFound() {
        when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> changeStoreStatusService.execute(new ChangeStoreStatusCommand(storeId, StoreStatus.SUSPENDED)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

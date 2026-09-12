package com.jaldishop.backend.store.application;

import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateStoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    private UpdateStoreService updateStoreService;

    private UUID merchantUserId;

    @BeforeEach
    void setUp() {
        updateStoreService = new UpdateStoreService(storeRepository);
        merchantUserId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Actualizar tienda exitosamente")
    void updateStoreSuccessfully() {
        Store existingStore = Store.create(
                merchantUserId,
                "Nombre Anterior",
                "slug-original",
                "Desc",
                null, null, null, null, null,
                true, false, null, null, false, null
        );

        UpdateStoreCommand command = new UpdateStoreCommand(
                merchantUserId,
                "Nuevo Nombre",
                "Nueva Desc",
                "+50212345678",
                "Nueva Direccion",
                "Nueva Ref",
                new BigDecimal("14.0"),
                new BigDecimal("-90.0"),
                true,
                true,
                new BigDecimal("20.00"),
                "GTQ",
                true,
                new BigDecimal("12.00")
        );

        when(storeRepository.findByMerchantUserId(merchantUserId)).thenReturn(Optional.of(existingStore));
        when(storeRepository.save(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Store updated = updateStoreService.execute(command);

        assertNotNull(updated);
        assertEquals("Nuevo Nombre", updated.getName());
        assertEquals("Nueva Desc", updated.getDescription());
        assertEquals("+50212345678", updated.getContactPhone());
        assertEquals("Nueva Direccion", updated.getAddress());
        assertEquals(new BigDecimal("20.00"), updated.getDeliveryFeeAmount());
        assertEquals(new BigDecimal("12.00"), updated.getTaxRate());
        verify(storeRepository).save(existingStore);
    }

    @Test
    @DisplayName("Lanzar excepción al intentar actualizar tienda inexistente")
    void throwExceptionWhenUpdatingNonExistentStore() {
        UpdateStoreCommand command = new UpdateStoreCommand(
                merchantUserId,
                "Nombre",
                null, null, null, null, null, null,
                true, false, null, null, false, null
        );

        when(storeRepository.findByMerchantUserId(merchantUserId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> updateStoreService.execute(command)
        );

        assertEquals("RESOURCE_NOT_FOUND", exception.getCode());
        verify(storeRepository, never()).save(any(Store.class));
    }
}

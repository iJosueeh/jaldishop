package com.jaldishop.backend.store.application;

import com.jaldishop.backend.media.domain.MediaStorageService;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.CloseStoreAction;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import com.jaldishop.backend.store.domain.StoreStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloseMyStoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private MediaStorageService mediaStorageService;

    private CloseMyStoreService closeMyStoreService;

    private UUID merchantUserId;
    private Store store;

    @BeforeEach
    void setUp() {
        closeMyStoreService = new CloseMyStoreService(storeRepository, mediaStorageService);
        merchantUserId = UUID.randomUUID();
        store = Store.create(
                merchantUserId,
                "Tienda Prueba",
                "tienda-prueba",
                null, null, null, null, null, null,
                true, false, null, null, null
        );
    }

    @Test
    @DisplayName("Eliminar tienda permanentemente cuando no tiene historial operativo")
    void hardDeleteStoreWhenNoOperationalHistory() {
        when(storeRepository.findByMerchantUserId(merchantUserId)).thenReturn(Optional.of(store));
        when(storeRepository.hasOperationalHistory(store.getId())).thenReturn(false);

        CloseStoreResult result = closeMyStoreService.execute(new CloseMyStoreCommand(merchantUserId, "Cierre voluntario sin actividad"));

        assertNotNull(result);
        assertEquals(store.getId(), result.storeId());
        assertEquals(CloseStoreAction.DELETED, result.action());
        assertNull(result.storeStatus());

        verify(storeRepository).deleteStore(store.getId(), merchantUserId);
        verify(mediaStorageService).deleteStoreMedia(store.getId());
        verify(storeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Desactivar tienda lógicamente cuando tiene historial operativo (pedidos o reservas)")
    void deactivateStoreWhenHasOperationalHistory() {
        when(storeRepository.findByMerchantUserId(merchantUserId)).thenReturn(Optional.of(store));
        when(storeRepository.hasOperationalHistory(store.getId())).thenReturn(true);
        when(storeRepository.save(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CloseStoreResult result = closeMyStoreService.execute(new CloseMyStoreCommand(merchantUserId, "Cierre de negocio"));

        assertNotNull(result);
        assertEquals(store.getId(), result.storeId());
        assertEquals(CloseStoreAction.DEACTIVATED, result.action());
        assertEquals(StoreStatus.INACTIVE, result.storeStatus());

        verify(storeRepository, never()).deleteStore(any(), any());
        verify(mediaStorageService, never()).deleteStoreMedia(any());
        verify(storeRepository).save(argThat(s -> s.getStatus() == StoreStatus.INACTIVE));
    }

    @Test
    @DisplayName("Lanzar excepción cuando el comerciante no tiene tienda registrada")
    void throwExceptionWhenStoreNotFound() {
        when(storeRepository.findByMerchantUserId(merchantUserId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> closeMyStoreService.execute(new CloseMyStoreCommand(merchantUserId, null))
        );

        assertEquals("RESOURCE_NOT_FOUND", exception.getCode());
        verify(storeRepository, never()).deleteStore(any(), any());
        verify(storeRepository, never()).save(any());
    }
}

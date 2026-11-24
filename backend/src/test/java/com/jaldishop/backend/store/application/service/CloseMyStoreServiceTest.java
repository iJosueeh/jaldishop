package com.jaldishop.backend.store.application.service;

import com.jaldishop.backend.store.application.command.CloseMyStoreCommand;
import com.jaldishop.backend.store.application.result.CloseStoreResult;

import com.jaldishop.backend.shared.exception.ResourceConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.enums.CloseStoreAction;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.port.StoreMediaCleaner;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
import com.jaldishop.backend.store.domain.enums.StoreStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
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
    private StoreMediaCleaner storeMediaCleaner;

    private CloseMyStoreService closeMyStoreService;

    private UUID merchantUserId;
    private Store store;

    @BeforeEach
    void setUp() {
        closeMyStoreService = new CloseMyStoreService(storeRepository, storeMediaCleaner);
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
        verify(storeMediaCleaner).deleteStoreMedia(store.getId());
        verify(storeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Bloquear hard delete con 409 STORE_HAS_OPERATIONAL_HISTORY cuando tiene historial operativo")
    void blockHardDeleteWith409WhenHasOperationalHistory() {
        when(storeRepository.findByMerchantUserId(merchantUserId)).thenReturn(Optional.of(store));
        when(storeRepository.lockByIdForDeletion(store.getId())).thenReturn(Optional.of(store));
        when(storeRepository.hasOperationalHistory(store.getId())).thenReturn(true);

        ResourceConflictException exception = assertThrows(
                ResourceConflictException.class,
                () -> closeMyStoreService.execute(new CloseMyStoreCommand(merchantUserId, "Cierre de negocio"))
        );

        assertEquals("STORE_HAS_OPERATIONAL_HISTORY", exception.getCode());
        assertEquals("DEACTIVATE", exception.getMetadata().get("suggestedAction"));
        assertEquals("ACTIVE", exception.getMetadata().get("currentStatus"));

        verify(storeRepository, never()).deleteStore(any(), any());
        verify(storeMediaCleaner, never()).deleteStoreMedia(any());
        verify(storeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Desactivar tienda lógicamente vía deactivate()")
    void deactivateStoreExplicitly() {
        when(storeRepository.findByMerchantUserId(merchantUserId)).thenReturn(Optional.of(store));
        when(storeRepository.save(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CloseStoreResult result = closeMyStoreService.deactivate(merchantUserId);

        assertEquals(CloseStoreAction.DEACTIVATED, result.action());
        assertEquals(StoreStatus.INACTIVE, result.storeStatus());
        verify(storeRepository).save(argThat(s -> s.getStatus() == StoreStatus.INACTIVE));
    }

    @Test
    @DisplayName("Reactivar tienda inactiva vía reactivate()")
    void reactivateInactiveStore() {
        store.deactivate();
        when(storeRepository.findByMerchantUserId(merchantUserId)).thenReturn(Optional.of(store));
        when(storeRepository.save(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Store reactivated = closeMyStoreService.reactivate(merchantUserId);

        assertEquals(StoreStatus.ACTIVE, reactivated.getStatus());
        verify(storeRepository).save(argThat(s -> s.getStatus() == StoreStatus.ACTIVE));
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

    @Test
    @DisplayName("Limpiar los medios de la tienda después de eliminarla, preservando el orden de operaciones")
    void cleanupStoreMediaAfterDeletionInOrder() {
        when(storeRepository.findByMerchantUserId(merchantUserId)).thenReturn(Optional.of(store));
        when(storeRepository.hasOperationalHistory(store.getId())).thenReturn(false);

        closeMyStoreService.execute(new CloseMyStoreCommand(merchantUserId, "Cierre sin actividad"));

        InOrder inOrder = inOrder(storeRepository, storeMediaCleaner);
        inOrder.verify(storeRepository).deleteStore(store.getId(), merchantUserId);
        inOrder.verify(storeMediaCleaner).deleteStoreMedia(store.getId());
    }

    @Test
    @DisplayName("Un fallo en la limpieza de medios no impide el cierre permanente de la tienda")
    void storeMediaCleanupFailureDoesNotPreventStoreDeletion() {
        when(storeRepository.findByMerchantUserId(merchantUserId)).thenReturn(Optional.of(store));
        when(storeRepository.hasOperationalHistory(store.getId())).thenReturn(false);
        doThrow(new RuntimeException("Cloudinary no disponible"))
                .when(storeMediaCleaner).deleteStoreMedia(store.getId());

        CloseStoreResult result = closeMyStoreService.execute(
                new CloseMyStoreCommand(merchantUserId, "Cierre sin actividad")
        );

        assertNotNull(result);
        assertEquals(CloseStoreAction.DELETED, result.action());
        assertNull(result.storeStatus());
        verify(storeRepository).deleteStore(store.getId(), merchantUserId);
        verify(storeMediaCleaner).deleteStoreMedia(store.getId());
    }
}

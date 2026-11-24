package com.jaldishop.backend.store.application.service;

import com.jaldishop.backend.identity.application.service.AdminAuditService;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.shared.exception.ResourceConflictException;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.port.StoreMediaCleaner;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
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
class AdminDeleteStoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreMediaCleaner storeMediaCleaner;

    @Mock
    private AdminAuditService auditService;

    @Mock
    private UserRepository userRepository;

    private AdminDeleteStoreService service;
    private Store store;
    private UUID adminId;

    @BeforeEach
    void setUp() {
        service = new AdminDeleteStoreService(storeRepository, storeMediaCleaner, auditService, userRepository);
        adminId = UUID.randomUUID();
        store = Store.create(UUID.randomUUID(), "Tienda Test", "tienda-test", null, null, null, null, null, null, true, false, null, null, null);
    }

    @Test
    @DisplayName("Bloquea borrado físico Admin con 409 STORE_HAS_OPERATIONAL_HISTORY y sugiere SUSPEND")
    void blockDeleteWhenStoreHasHistory() {
        when(storeRepository.lockByIdForDeletion(store.getId())).thenReturn(Optional.of(store));
        when(storeRepository.hasOperationalHistory(store.getId())).thenReturn(true);

        ResourceConflictException ex = assertThrows(ResourceConflictException.class, () -> service.execute(store.getId(), adminId));

        assertEquals("STORE_HAS_OPERATIONAL_HISTORY", ex.getCode());
        assertEquals("SUSPEND", ex.getMetadata().get("suggestedAction"));
        verify(storeRepository, never()).deleteStore(any(), any());
    }

    @Test
    @DisplayName("Elimina tienda sin historial y registra snapshot STORE_DELETED en auditoría")
    void deleteCleanStoreAndLogSnapshot() {
        when(storeRepository.lockByIdForDeletion(store.getId())).thenReturn(Optional.of(store));
        when(storeRepository.hasOperationalHistory(store.getId())).thenReturn(false);

        service.execute(store.getId(), adminId);

        verify(storeRepository).deleteStore(store.getId(), store.getOwnerUserId());
        verify(storeMediaCleaner).deleteStoreMedia(store.getId());
        verify(auditService).logSuccess(eq(adminId), any(), eq("STORE_DELETED"), eq("STORE"), eq(store.getId()), isNull(), contains("tienda-test"));
    }
}

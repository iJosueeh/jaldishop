package com.jaldishop.backend.store.application;

import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreContextServiceTest {

    @Mock
    private GetMyStoreService getMyStoreService;

    private StoreContextService storeContextService;

    private UUID userId;
    private UUID storeId;
    private Store mockStore;

    @BeforeEach
    void setUp() {
        storeContextService = new StoreContextService(getMyStoreService);
        userId = UUID.randomUUID();
        storeId = UUID.randomUUID();

        mockStore = Store.reconstitute(
                storeId,
                userId,
                "Mi Tienda de Prueba",
                "mi-tienda-prueba",
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                false,
                null,
                null,
                false,
                null,
                StoreStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("requireStoreId debe retornar el UUID de la tienda si el usuario es MERCHANT")
    void requireStoreIdSuccess() {
        JwtPrincipal principal = new JwtPrincipal(userId, Set.of("MERCHANT"));
        when(getMyStoreService.execute(userId)).thenReturn(mockStore);

        UUID result = storeContextService.requireStoreId(principal);

        assertNotNull(result);
        assertEquals(storeId, result);
    }

    @Test
    @DisplayName("requireStoreId debe lanzar AccessDeniedException si el usuario no tiene rol MERCHANT")
    void requireStoreIdThrowsWhenNotMerchant() {
        JwtPrincipal principal = new JwtPrincipal(userId, Set.of("CUSTOMER"));

        assertThrows(AccessDeniedException.class, () -> storeContextService.requireStoreId(principal));
    }

    @Test
    @DisplayName("requireStoreId debe lanzar AccessDeniedException si el principal es nulo")
    void requireStoreIdThrowsWhenPrincipalNull() {
        assertThrows(AccessDeniedException.class, () -> storeContextService.requireStoreId(null));
    }

    @Test
    @DisplayName("requireStore debe retornar la entidad Store si el usuario es MERCHANT")
    void requireStoreSuccess() {
        JwtPrincipal principal = new JwtPrincipal(userId, Set.of("MERCHANT", "CUSTOMER"));
        when(getMyStoreService.execute(userId)).thenReturn(mockStore);

        Store result = storeContextService.requireStore(principal);

        assertNotNull(result);
        assertEquals(mockStore, result);
    }

    @Test
    @DisplayName("validateStoreOwnership no debe lanzar excepción cuando el storeId coincide")
    void validateStoreOwnershipSuccess() {
        JwtPrincipal principal = new JwtPrincipal(userId, Set.of("MERCHANT"));
        when(getMyStoreService.execute(userId)).thenReturn(mockStore);

        assertDoesNotThrow(() -> storeContextService.validateStoreOwnership(storeId, principal));
    }

    @Test
    @DisplayName("validateStoreOwnership debe lanzar AccessDeniedException cuando el storeId no coincide")
    void validateStoreOwnershipThrowsWhenMismatch() {
        JwtPrincipal principal = new JwtPrincipal(userId, Set.of("MERCHANT"));
        when(getMyStoreService.execute(userId)).thenReturn(mockStore);

        UUID differentStoreId = UUID.randomUUID();
        assertThrows(AccessDeniedException.class, () ->
                storeContextService.validateStoreOwnership(differentStoreId, principal)
        );
    }
}

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

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetMyStoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    private GetMyStoreService getMyStoreService;

    private UUID merchantUserId;

    @BeforeEach
    void setUp() {
        getMyStoreService = new GetMyStoreService(storeRepository);
        merchantUserId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Obtener tienda cuando existe")
    void getMyStoreSuccessfully() {
        Store store = Store.create(
                merchantUserId,
                "Mi Tienda",
                "mi-tienda",
                null, null, null, null, null, null,
                true, false, null, null, false, null
        );

        when(storeRepository.findByMerchantUserId(merchantUserId)).thenReturn(Optional.of(store));

        Store result = getMyStoreService.execute(merchantUserId);

        assertNotNull(result);
        assertEquals("Mi Tienda", result.getName());
        assertEquals(merchantUserId, result.getMerchantUserId());
        verify(storeRepository).findByMerchantUserId(merchantUserId);
    }

    @Test
    @DisplayName("Lanzar excepción cuando el comerciante no tiene tienda registrada")
    void throwExceptionWhenStoreNotFound() {
        when(storeRepository.findByMerchantUserId(merchantUserId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> getMyStoreService.execute(merchantUserId)
        );

        assertEquals("RESOURCE_NOT_FOUND", exception.getCode());
        verify(storeRepository).findByMerchantUserId(merchantUserId);
    }
}

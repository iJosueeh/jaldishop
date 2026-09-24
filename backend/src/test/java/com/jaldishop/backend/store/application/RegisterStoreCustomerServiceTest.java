package com.jaldishop.backend.store.application;

import com.jaldishop.backend.store.domain.StoreCustomer;
import com.jaldishop.backend.store.domain.StoreCustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterStoreCustomerServiceTest {

    @Mock
    private StoreCustomerRepository storeCustomerRepository;

    private RegisterStoreCustomerService registerStoreCustomerService;

    @BeforeEach
    void setUp() {
        registerStoreCustomerService = new RegisterStoreCustomerService(storeCustomerRepository);
    }

    @Test
    void shouldRegisterNewCustomerWhenNotAlreadyRegistered() {
        UUID storeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(storeCustomerRepository.existsByStoreIdAndUserId(storeId, userId)).thenReturn(false);

        registerStoreCustomerService.execute(storeId, userId);

        ArgumentCaptor<StoreCustomer> captor = ArgumentCaptor.forClass(StoreCustomer.class);
        verify(storeCustomerRepository, times(1)).save(captor.capture());

        StoreCustomer saved = captor.getValue();
        assertEquals(storeId, saved.getStoreId());
        assertEquals(userId, saved.getUserId());
    }

    @Test
    void shouldDoNothingWhenCustomerIsAlreadyRegistered() {
        UUID storeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(storeCustomerRepository.existsByStoreIdAndUserId(storeId, userId)).thenReturn(true);

        registerStoreCustomerService.execute(storeId, userId);

        verify(storeCustomerRepository, never()).save(any());
    }

    @Test
    void shouldDoNothingWhenIdsAreNull() {
        registerStoreCustomerService.execute(null, UUID.randomUUID());
        registerStoreCustomerService.execute(UUID.randomUUID(), null);

        verify(storeCustomerRepository, never()).existsByStoreIdAndUserId(any(), any());
        verify(storeCustomerRepository, never()).save(any());
    }
}

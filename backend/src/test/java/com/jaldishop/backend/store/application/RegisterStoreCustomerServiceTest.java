package com.jaldishop.backend.store.application;

import com.jaldishop.backend.store.domain.StoreCustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

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
    void shouldRegisterCustomer() {
        UUID storeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        registerStoreCustomerService.execute(storeId, userId);

        verify(storeCustomerRepository, times(1)).registerCustomer(storeId, userId);
    }

    @Test
    void shouldDoNothingWhenIdsAreNull() {
        registerStoreCustomerService.execute(null, UUID.randomUUID());
        registerStoreCustomerService.execute(UUID.randomUUID(), null);

        verify(storeCustomerRepository, never()).registerCustomer(any(), any());
    }
}

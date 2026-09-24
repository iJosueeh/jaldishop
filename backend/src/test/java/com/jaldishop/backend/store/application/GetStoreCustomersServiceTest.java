package com.jaldishop.backend.store.application;

import com.jaldishop.backend.store.domain.StoreCustomerRepository;
import com.jaldishop.backend.store.web.dto.StoreCustomerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetStoreCustomersServiceTest {

    @Mock
    private StoreCustomerRepository storeCustomerRepository;

    private GetStoreCustomersService getStoreCustomersService;

    @BeforeEach
    void setUp() {
        getStoreCustomersService = new GetStoreCustomersService(storeCustomerRepository);
    }

    @Test
    void shouldReturnCustomersList() {
        UUID storeId = UUID.randomUUID();
        StoreCustomerResponse response = new StoreCustomerResponse(
                UUID.randomUUID(),
                "Valeria",
                "Ramos",
                "valeria@example.com",
                "+51984552109",
                Instant.now(),
                3L,
                new BigDecimal("150.00"),
                Instant.now()
        );

        when(storeCustomerRepository.findCustomersByStoreId(storeId, "valeria")).thenReturn(List.of(response));

        List<StoreCustomerResponse> result = getStoreCustomersService.execute(storeId, "valeria");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Valeria", result.get(0).firstName());
        assertEquals(3L, result.get(0).ordersCount());
        verify(storeCustomerRepository, times(1)).findCustomersByStoreId(storeId, "valeria");
    }

    @Test
    void shouldThrowExceptionWhenStoreIdIsNull() {
        assertThrows(IllegalArgumentException.class, () -> getStoreCustomersService.execute(null, null));
    }
}

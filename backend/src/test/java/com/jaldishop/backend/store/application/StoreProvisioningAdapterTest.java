package com.jaldishop.backend.store.application;

import com.jaldishop.backend.identity.application.StoreProvisioningData;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.store.domain.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Verifica que el adaptador de aprovisionamiento de tienda es un mapeo 1:1
 * burdo de los 20 campos hacia CreateStoreCommand, sin lógica propia.
 */
@ExtendWith(MockitoExtension.class)
class StoreProvisioningAdapterTest {

    @Mock
    private CreateStoreService createStoreService;

    @Mock
    private Store store;

    private StoreProvisioningAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new StoreProvisioningAdapter(createStoreService);
    }

    private StoreProvisioningData sampleData() {
        return new StoreProvisioningData(
                UUID.randomUUID(),
                "Pastelería Dulce",
                "pasteleria-dulce",
                "Repostería",
                "911111111",
                "Av. Principal 123",
                "Jr. Secundaria 456",
                new BigDecimal("-12.0464"),
                new BigDecimal("-77.0428"),
                true,
                false,
                new BigDecimal("5.00"),
                "PEN",
                new BigDecimal("18.00"),
                "https://cdn/logo.png",
                "https://cdn/banner.png",
                "https://instagram.com/pasteleria",
                "https://facebook.com/pasteleria",
                "911111111",
                Set.of(UUID.randomUUID())
        );
    }

    @Test
    @DisplayName("Mapear los 20 campos de StoreProvisioningData a CreateStoreCommand en el mismo orden")
    void shouldMapAllTwentyFieldsOneToOne() {
        StoreProvisioningData data = sampleData();
        when(createStoreService.execute(any(CreateStoreCommand.class))).thenReturn(store);

        adapter.provisionStore(data);

        ArgumentCaptor<CreateStoreCommand> captor = ArgumentCaptor.forClass(CreateStoreCommand.class);
        verify(createStoreService).execute(captor.capture());
        CreateStoreCommand command = captor.getValue();

        assertEquals(data.ownerUserId(), command.ownerUserId());
        assertEquals(data.name(), command.name());
        assertEquals(data.slug(), command.slug());
        assertEquals(data.description(), command.description());
        assertEquals(data.contactPhone(), command.contactPhone());
        assertEquals(data.address(), command.address());
        assertEquals(data.addressReference(), command.addressReference());
        assertEquals(0, data.latitude().compareTo(command.latitude()));
        assertEquals(0, data.longitude().compareTo(command.longitude()));
        assertEquals(data.pickupEnabled(), command.pickupEnabled());
        assertEquals(data.deliveryEnabled(), command.deliveryEnabled());
        assertEquals(0, data.deliveryFeeAmount().compareTo(command.deliveryFeeAmount()));
        assertEquals(data.deliveryFeeCurrency(), command.deliveryFeeCurrency());
        assertEquals(0, data.taxRate().compareTo(command.taxRate()));
        assertEquals(data.logoUrl(), command.logoUrl());
        assertEquals(data.bannerUrl(), command.bannerUrl());
        assertEquals(data.instagramUrl(), command.instagramUrl());
        assertEquals(data.facebookUrl(), command.facebookUrl());
        assertEquals(data.whatsappNumber(), command.whatsappNumber());
        assertEquals(data.categoryIds(), command.categoryIds());
    }

    @Test
    @DisplayName("Delegar en CreateStoreService y devolver el id de la tienda creada")
    void shouldDelegateToCreateStoreServiceAndReturnStoreId() {
        StoreProvisioningData data = sampleData();
        UUID storeId = UUID.randomUUID();
        when(createStoreService.execute(any(CreateStoreCommand.class))).thenReturn(store);
        when(store.getId()).thenReturn(storeId);

        UUID result = adapter.provisionStore(data);

        assertEquals(storeId, result);
        verify(createStoreService, times(1)).execute(any(CreateStoreCommand.class));
    }

    @Test
    @DisplayName("Propagar la excepción de CreateStoreService sin traducirla ni capturarla")
    void shouldPropagateCreateStoreFailure() {
        StoreProvisioningData data = sampleData();
        when(createStoreService.execute(any(CreateStoreCommand.class)))
                .thenThrow(new ConflictException("STORE_SLUG_ALREADY_EXISTS", "El slug ya existe"));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> adapter.provisionStore(data)
        );

        assertEquals("STORE_SLUG_ALREADY_EXISTS", exception.getCode());
        verify(store, never()).getId();
    }
}

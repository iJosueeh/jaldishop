package com.jaldishop.backend.address.application;

import com.jaldishop.backend.address.domain.CustomerAddress;
import com.jaldishop.backend.address.domain.CustomerAddressRepository;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteCustomerAddressServiceTest {

    @Mock
    private CustomerAddressRepository addressRepository;

    @InjectMocks
    private DeleteCustomerAddressService service;

    private UUID userId;
    private UUID addressId;
    private CustomerAddress address;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        addressId = UUID.randomUUID();
        address = new CustomerAddress(
                addressId, userId, "Casa", "Juan Pérez", "987654321",
                "Calle 1", "Piura", "Morropon", "Chulucanas", "200401",
                null, null, null, true, Instant.now(), Instant.now()
        );
    }

    @Test
    @DisplayName("Elimina la dirección predeterminada y promueve la siguiente restante a predeterminada")
    void shouldDeleteDefaultAndPromoteNext() {
        UUID otherId = UUID.randomUUID();
        CustomerAddress remaining = new CustomerAddress(
                otherId, userId, "Taller", "Juan Pérez", "987654321",
                "Calle 2", "Piura", "Morropon", "Chulucanas", "200401",
                null, null, null, false, Instant.now(), Instant.now()
        );

        when(addressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.of(address));
        when(addressRepository.findMostRecentlyUpdated(userId)).thenReturn(Optional.of(remaining));

        service.execute(userId, addressId);

        verify(addressRepository).lockUserForUpdate(userId);
        verify(addressRepository).delete(address);
        verify(addressRepository).findMostRecentlyUpdated(userId);
        verify(addressRepository).save(remaining);
    }

    @Test
    @DisplayName("Lanza ResourceNotFoundException si la dirección a eliminar no pertenece al usuario")
    void shouldThrowNotFoundWhenNotOwned() {
        when(addressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(userId, addressId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(addressRepository, never()).delete(any());
    }
}

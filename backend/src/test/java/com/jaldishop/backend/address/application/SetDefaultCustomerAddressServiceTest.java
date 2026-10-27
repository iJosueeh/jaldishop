package com.jaldishop.backend.address.application;

import com.jaldishop.backend.address.domain.CustomerAddress;
import com.jaldishop.backend.address.domain.CustomerAddressRepository;
import com.jaldishop.backend.address.web.dto.CustomerAddressResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SetDefaultCustomerAddressServiceTest {

    @Mock
    private CustomerAddressRepository addressRepository;

    @InjectMocks
    private SetDefaultCustomerAddressService service;

    private UUID userId;
    private UUID addressId;
    private CustomerAddress address;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        addressId = UUID.randomUUID();
        address = new CustomerAddress(
                addressId, userId, "Oficina", "Juan Pérez", "987654321",
                "Calle 2", "Arequipa", "Arequipa", "Yanahuara", "040126",
                null, null, null, false, Instant.now(), Instant.now()
        );
    }

    @Test
    @DisplayName("Desmarca la anterior predeterminada y marca la nueva de forma atómica")
    void shouldSetDefaultAddressSuccessfully() {
        when(addressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.of(address));
        when(addressRepository.save(any(CustomerAddress.class))).thenAnswer(i -> i.getArgument(0));

        CustomerAddressResponse response = service.execute(userId, addressId);

        assertThat(response.isDefault()).isTrue();
        verify(addressRepository).lockUserForUpdate(userId);
        verify(addressRepository).clearDefaultForUser(userId);
        verify(addressRepository).save(address);
    }

    @Test
    @DisplayName("Lanza ResourceNotFoundException si la dirección no pertenece al usuario")
    void shouldThrowNotFoundWhenNotOwned() {
        when(addressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(userId, addressId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(addressRepository, never()).clearDefaultForUser(userId);
    }
}

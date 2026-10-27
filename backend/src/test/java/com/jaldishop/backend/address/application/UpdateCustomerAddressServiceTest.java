package com.jaldishop.backend.address.application;

import com.jaldishop.backend.address.domain.CustomerAddress;
import com.jaldishop.backend.address.domain.CustomerAddressRepository;
import com.jaldishop.backend.address.infrastructure.ubigeo.UbigeoCatalogService;
import com.jaldishop.backend.address.web.dto.CustomerAddressResponse;
import com.jaldishop.backend.address.web.dto.UpdateCustomerAddressRequest;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateCustomerAddressServiceTest {

    @Mock
    private CustomerAddressRepository addressRepository;

    @Mock
    private UbigeoCatalogService ubigeoCatalogService;

    @InjectMocks
    private UpdateCustomerAddressService service;

    private UUID userId;
    private UUID addressId;
    private CustomerAddress existingAddress;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        addressId = UUID.randomUUID();
        existingAddress = new CustomerAddress(
                addressId, userId, "Casa", "Juan Pérez", "987654321",
                "Av. Pardo 100", "Lima", "Lima", "Miraflores", "150122",
                null, null, null, true, Instant.now(), Instant.now()
        );
    }

    @Test
    @DisplayName("Actualiza exitosamente los datos de una dirección del cliente")
    void shouldUpdateAddressSuccessfully() {
        UpdateCustomerAddressRequest request = new UpdateCustomerAddressRequest(
                "Mi Casa", "Juan Pérez Actualizado", "987654321", "Av. Pardo 200",
                "Lima", "Lima", "Miraflores", "150122",
                "Piso 4", new BigDecimal("-12.12"), new BigDecimal("-77.02"), true
        );

        when(addressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.of(existingAddress));
        when(ubigeoCatalogService.isValidHierarchy("150122", "Lima", "Lima", "Miraflores")).thenReturn(true);
        when(addressRepository.countByUserId(userId)).thenReturn(2L);
        when(addressRepository.save(any(CustomerAddress.class))).thenAnswer(i -> i.getArgument(0));

        CustomerAddressResponse response = service.execute(userId, addressId, request);

        assertThat(response.alias()).isEqualTo("Mi Casa");
        assertThat(response.recipientName()).isEqualTo("Juan Pérez Actualizado");
        verify(addressRepository).save(existingAddress);
    }

    @Test
    @DisplayName("Lanza ResourceNotFoundException si la dirección no pertenece al usuario autenticado (IDOR safe)")
    void shouldThrowNotFoundWhenAddressBelongsToAnotherUser() {
        UpdateCustomerAddressRequest request = new UpdateCustomerAddressRequest(
                "Casa", "Juan", "987654321", "Calle 1",
                "Lima", "Lima", "Miraflores", "150122",
                null, null, null, false
        );

        when(addressRepository.findByIdAndUserId(addressId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(userId, addressId, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(addressRepository, never()).save(any(CustomerAddress.class));
    }
}

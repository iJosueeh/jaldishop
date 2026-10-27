package com.jaldishop.backend.address.application;

import com.jaldishop.backend.address.domain.CustomerAddress;
import com.jaldishop.backend.address.domain.CustomerAddressRepository;
import com.jaldishop.backend.address.infrastructure.ubigeo.UbigeoCatalogService;
import com.jaldishop.backend.address.web.dto.CreateCustomerAddressRequest;
import com.jaldishop.backend.address.web.dto.CustomerAddressResponse;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateCustomerAddressServiceTest {

    @Mock
    private CustomerAddressRepository addressRepository;

    @Mock
    private UbigeoCatalogService ubigeoCatalogService;

    @InjectMocks
    private CreateCustomerAddressService service;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Crea exitosamente la primera dirección y la establece como predeterminada automáticamente")
    void shouldCreateFirstAddressAsDefaultAutomatically() {
        CreateCustomerAddressRequest request = new CreateCustomerAddressRequest(
                "Casa", "Juan Pérez", "987654321", "Av. Larco 123",
                "Lima", "Lima", "Miraflores", "150122",
                "Frente al parque", new BigDecimal("-12.1215"), new BigDecimal("-77.0298"), false
        );

        when(addressRepository.countByUserId(userId)).thenReturn(0L);
        when(ubigeoCatalogService.isValidHierarchy("150122", "Lima", "Lima", "Miraflores")).thenReturn(true);
        when(addressRepository.save(any(CustomerAddress.class))).thenAnswer(i -> i.getArgument(0));

        CustomerAddressResponse response = service.execute(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.alias()).isEqualTo("Casa");
        assertThat(response.isDefault()).isTrue();
        assertThat(response.ubigeoCode()).isEqualTo("150122");

        verify(addressRepository).lockUserForUpdate(userId);
        verify(addressRepository, never()).clearDefaultForUser(userId);
        verify(addressRepository).save(any(CustomerAddress.class));
    }

    @Test
    @DisplayName("Lanza BusinessRuleException con ADDRESS_LIMIT_EXCEEDED cuando el cliente ya tiene 5 direcciones")
    void shouldThrowWhenLimitExceeded() {
        CreateCustomerAddressRequest request = new CreateCustomerAddressRequest(
                "Taller", "Juan Pérez", "987654321", "Calle 1",
                "Arequipa", "Arequipa", "Yanahuara", "040126",
                null, null, null, false
        );

        when(addressRepository.countByUserId(userId)).thenReturn(5L);

        assertThatThrownBy(() -> service.execute(userId, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("límite máximo de 5 direcciones");

        verify(addressRepository).lockUserForUpdate(userId);
        verify(addressRepository, never()).save(any(CustomerAddress.class));
    }

    @Test
    @DisplayName("Lanza BusinessRuleException con INVALID_UBIGEO si el código no coincide con la jerarquía de INEI")
    void shouldThrowWhenUbigeoIsInvalid() {
        CreateCustomerAddressRequest request = new CreateCustomerAddressRequest(
                "Oficina", "Juan Pérez", "987654321", "Av. Principal 456",
                "Cusco", "Cusco", "Wanchaq", "999999",
                null, null, null, false
        );

        when(addressRepository.countByUserId(userId)).thenReturn(2L);
        when(ubigeoCatalogService.isValidHierarchy("999999", "Cusco", "Cusco", "Wanchaq")).thenReturn(false);

        assertThatThrownBy(() -> service.execute(userId, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("catálogo oficial de INEI");

        verify(addressRepository, never()).save(any(CustomerAddress.class));
    }
}

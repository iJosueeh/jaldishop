package com.jaldishop.backend.address.web.controller;

import tools.jackson.databind.ObjectMapper;
import com.jaldishop.backend.address.application.*;
import com.jaldishop.backend.address.web.dto.*;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CustomerAddressControllerTest {

    @Mock
    private GetCustomerAddressesService getCustomerAddressesService;

    @Mock
    private CreateCustomerAddressService createCustomerAddressService;

    @Mock
    private UpdateCustomerAddressService updateCustomerAddressService;

    @Mock
    private SetDefaultCustomerAddressService setDefaultCustomerAddressService;

    @Mock
    private DeleteCustomerAddressService deleteCustomerAddressService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID customerUserId;
    private JwtPrincipal customerPrincipal;

    @BeforeEach
    void setUp() {
        customerUserId = UUID.randomUUID();
        customerPrincipal = new JwtPrincipal(customerUserId, Set.of("CUSTOMER"));
        objectMapper = new ObjectMapper();

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                        && parameter.getParameterType().isAssignableFrom(JwtPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                return customerPrincipal;
            }
        };

        CustomerAddressController controller = new CustomerAddressController(
                getCustomerAddressesService,
                createCustomerAddressService,
                updateCustomerAddressService,
                setDefaultCustomerAddressService,
                deleteCustomerAddressService
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/customer/addresses retorna 200 con la lista de direcciones")
    void shouldGetAddresses() throws Exception {
        CustomerAddressResponse addr = new CustomerAddressResponse(
                UUID.randomUUID(), "Casa", "Juan Pérez", "987654321",
                "Av. Larco 456", "Lima", "Lima", "Miraflores", "150122",
                "Frente al parque", new BigDecimal("-12.12"), new BigDecimal("-77.02"),
                true, Instant.now(), Instant.now()
        );
        CustomerAddressListResponse listResponse = new CustomerAddressListResponse(1, 5, List.of(addr));

        when(getCustomerAddressesService.execute(customerUserId)).thenReturn(listResponse);

        mockMvc.perform(get("/api/v1/customer/addresses")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.maxAllowed").value(5))
                .andExpect(jsonPath("$.addresses[0].alias").value("Casa"))
                .andExpect(jsonPath("$.addresses[0].ubigeoCode").value("150122"));
    }

    @Test
    @DisplayName("POST /api/v1/customer/addresses crea dirección y retorna 201 Created")
    void shouldCreateAddress() throws Exception {
        CreateCustomerAddressRequest request = new CreateCustomerAddressRequest(
                "Casa", "Juan Pérez", "987654321", "Av. Larco 456",
                "Lima", "Lima", "Miraflores", "150122",
                null, null, null, true
        );
        CustomerAddressResponse response = new CustomerAddressResponse(
                UUID.randomUUID(), "Casa", "Juan Pérez", "987654321",
                "Av. Larco 456", "Lima", "Lima", "Miraflores", "150122",
                null, null, null, true, Instant.now(), Instant.now()
        );

        when(createCustomerAddressService.execute(eq(customerUserId), any(CreateCustomerAddressRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/customer/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alias").value("Casa"))
                .andExpect(jsonPath("$.isDefault").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/customer/addresses retorna 422 cuando se excede el límite de 5")
    void shouldReturn422WhenLimitExceeded() throws Exception {
        CreateCustomerAddressRequest request = new CreateCustomerAddressRequest(
                "Taller", "Juan Pérez", "987654321", "Calle 1",
                "Lima", "Lima", "Miraflores", "150122",
                null, null, null, false
        );

        when(createCustomerAddressService.execute(eq(customerUserId), any(CreateCustomerAddressRequest.class)))
                .thenThrow(new BusinessRuleException("ADDRESS_LIMIT_EXCEEDED", "Has alcanzado el límite máximo de 5 direcciones."));

        mockMvc.perform(post("/api/v1/customer/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("PATCH /api/v1/customer/addresses/{id}/default marca como predeterminada y retorna 200")
    void shouldSetDefault() throws Exception {
        UUID addressId = UUID.randomUUID();
        CustomerAddressResponse response = new CustomerAddressResponse(
                addressId, "Oficina", "Juan Pérez", "987654321",
                "Calle 1", "Lima", "Lima", "Miraflores", "150122",
                null, null, null, true, Instant.now(), Instant.now()
        );

        when(setDefaultCustomerAddressService.execute(customerUserId, addressId)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/customer/addresses/{id}/default", addressId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDefault").value(true));
    }

    @Test
    @DisplayName("DELETE /api/v1/customer/addresses/{id} elimina y retorna 204 No Content")
    void shouldDeleteAddress() throws Exception {
        UUID addressId = UUID.randomUUID();
        doNothing().when(deleteCustomerAddressService).execute(customerUserId, addressId);

        mockMvc.perform(delete("/api/v1/customer/addresses/{id}", addressId))
                .andExpect(status().isNoContent());
    }
}

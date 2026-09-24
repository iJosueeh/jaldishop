package com.jaldishop.backend.store.web.controller;

import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
import com.jaldishop.backend.store.application.GetStoreCustomersService;
import com.jaldishop.backend.store.application.StoreContextService;
import com.jaldishop.backend.store.web.dto.StoreCustomerResponse;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MerchantCustomerControllerTest {

    @Mock
    private StoreContextService storeContextService;

    @Mock
    private GetStoreCustomersService getStoreCustomersService;

    private MockMvc mockMvc;
    private UUID testMerchantId;
    private UUID testStoreId;
    private JwtPrincipal currentPrincipal;

    @BeforeEach
    void setUp() {
        testMerchantId = UUID.randomUUID();
        testStoreId = UUID.randomUUID();
        currentPrincipal = new JwtPrincipal(testMerchantId, Set.of("MERCHANT"));

        MerchantCustomerController controller = new MerchantCustomerController(
                storeContextService,
                getStoreCustomersService
        );

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return currentPrincipal;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/merchant/customers - Retorna 200 OK con lista de clientes de la tienda")
    void shouldReturnCustomersList() throws Exception {
        UUID customerId = UUID.randomUUID();
        StoreCustomerResponse response = new StoreCustomerResponse(
                customerId,
                "Carlos",
                "García",
                "carlos@example.com",
                "+51987654321",
                Instant.parse("2026-09-01T10:00:00Z"),
                2L,
                new BigDecimal("85.50"),
                Instant.parse("2026-09-15T15:30:00Z")
        );

        when(storeContextService.requireStoreId(any())).thenReturn(testStoreId);
        when(getStoreCustomersService.execute(testStoreId, null)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/merchant/customers")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(customerId.toString()))
                .andExpect(jsonPath("$[0].firstName").value("Carlos"))
                .andExpect(jsonPath("$[0].ordersCount").value(2))
                .andExpect(jsonPath("$[0].totalSpentAmount").value(85.50));
    }
}

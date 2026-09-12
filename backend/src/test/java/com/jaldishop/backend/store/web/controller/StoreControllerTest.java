package com.jaldishop.backend.store.web.controller;

import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
import com.jaldishop.backend.store.application.CreateStoreCommand;
import com.jaldishop.backend.store.application.CreateStoreService;
import com.jaldishop.backend.store.application.GetMyStoreService;
import com.jaldishop.backend.store.application.UpdateStoreService;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreStatus;
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
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class StoreControllerTest {

    @Mock
    private CreateStoreService createStoreService;

    @Mock
    private GetMyStoreService getMyStoreService;

    @Mock
    private UpdateStoreService updateStoreService;

    private MockMvc mockMvc;
    private UUID testMerchantId;
    private JwtPrincipal currentPrincipal;

    @BeforeEach
    void setUp() {
        testMerchantId = UUID.randomUUID();
        currentPrincipal = new JwtPrincipal(testMerchantId, Set.of("MERCHANT"));

        StoreController controller = new StoreController(createStoreService, getMyStoreService, updateStoreService);

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                        && parameter.getParameterType().isAssignableFrom(JwtPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return currentPrincipal;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authPrincipalResolver)
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/stores - Crear tienda exitosamente (201 CREATED)")
    void createStoreSuccess() throws Exception {
        Store store = Store.reconstitute(
                UUID.randomUUID(),
                testMerchantId,
                "Tienda Jaldi",
                "tienda-jaldi",
                "Descripción",
                "+50212345678",
                "Zona 1",
                "Ref",
                new BigDecimal("14.500000"),
                new BigDecimal("-90.500000"),
                true,
                true,
                new BigDecimal("15.00"),
                "GTQ",
                false,
                null,
                StoreStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );

        when(createStoreService.execute(any(CreateStoreCommand.class))).thenReturn(store);

        String jsonRequest = """
                {
                    "name": "Tienda Jaldi",
                    "slug": "tienda-jaldi",
                    "description": "Descripción",
                    "contactPhone": "+51999999999",
                    "address": "Zona 1",
                    "addressReference": "Ref",
                    "latitude": 14.5,
                    "longitude": -90.5,
                    "pickupEnabled": true,
                    "deliveryEnabled": true,
                    "deliveryFeeAmount": 15.00,
                    "deliveryFeeCurrency": "GTQ",
                    "taxApplies": false
                }
                """;

        mockMvc.perform(post("/api/v1/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Tienda Jaldi"))
                .andExpect(jsonPath("$.slug").value("tienda-jaldi"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/v1/stores - 403 Forbidden cuando el usuario no tiene rol MERCHANT")
    void createStoreForbiddenWhenNotMerchant() throws Exception {
        currentPrincipal = new JwtPrincipal(testMerchantId, Set.of("CUSTOMER"));

        String jsonRequest = """
                {
                    "name": "Tienda Intruso",
                    "pickupEnabled": true,
                    "deliveryEnabled": false,
                    "taxApplies": false
                }
                """;

        mockMvc.perform(post("/api/v1/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("POST /api/v1/stores - 400 Bad Request cuando el nombre está vacío")
    void createStoreValidationErrorWhenNameIsBlank() throws Exception {
        String jsonRequest = """
                {
                    "name": "   ",
                    "pickupEnabled": true,
                    "deliveryEnabled": false,
                    "taxApplies": false
                }
                """;

        mockMvc.perform(post("/api/v1/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    @DisplayName("GET /api/v1/stores/me - Consultar mi tienda (200 OK)")
    void getMyStoreSuccess() throws Exception {
        Store store = Store.reconstitute(
                UUID.randomUUID(),
                testMerchantId,
                "Mi Tienda",
                "mi-tienda",
                null, null, null, null, null, null,
                true, false, null, null, false, null,
                StoreStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );

        when(getMyStoreService.execute(testMerchantId)).thenReturn(store);

        mockMvc.perform(get("/api/v1/stores/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mi Tienda"))
                .andExpect(jsonPath("$.merchantUserId").value(testMerchantId.toString()));
    }

    @Test
    @DisplayName("PUT /api/v1/stores/me - Actualizar mi tienda (200 OK)")
    void updateMyStoreSuccess() throws Exception {
        Store updatedStore = Store.reconstitute(
                UUID.randomUUID(),
                testMerchantId,
                "Nombre Actualizado",
                "slug-fijo",
                "Nueva Desc",
                "+51999999999",
                "Nueva Direccion",
                null, null, null,
                true, true, new BigDecimal("20.00"), "GTQ", false, null,
                StoreStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );

        when(updateStoreService.execute(any())).thenReturn(updatedStore);

        String jsonRequest = """
                {
                    "name": "Nombre Actualizado",
                    "description": "Nueva Desc",
                    "contactPhone": "+51999999999",
                    "address": "Nueva Direccion",
                    "pickupEnabled": true,
                    "deliveryEnabled": true,
                    "deliveryFeeAmount": 20.00,
                    "deliveryFeeCurrency": "GTQ",
                    "taxApplies": false
                }
                """;

        mockMvc.perform(put("/api/v1/stores/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nombre Actualizado"))
                .andExpect(jsonPath("$.description").value("Nueva Desc"));
    }
}

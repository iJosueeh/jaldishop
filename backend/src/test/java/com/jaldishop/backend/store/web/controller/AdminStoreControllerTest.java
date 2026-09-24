package com.jaldishop.backend.store.web.controller;

import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
import com.jaldishop.backend.store.application.ChangeStoreStatusService;
import com.jaldishop.backend.store.application.GetAdminStoresService;
import com.jaldishop.backend.store.domain.StoreStatus;
import com.jaldishop.backend.store.web.dto.AdminStoreDetailResponse;
import com.jaldishop.backend.store.web.dto.AdminStoreSummaryResponse;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminStoreControllerTest {

    @Mock
    private GetAdminStoresService getAdminStoresService;

    @Mock
    private ChangeStoreStatusService changeStoreStatusService;

    private MockMvc mockMvc;
    private UUID storeId;
    private UUID merchantId;
    private JwtPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        merchantId = UUID.randomUUID();
        adminPrincipal = new JwtPrincipal(UUID.randomUUID(), Set.of("ADMIN"));

        AdminStoreController controller = new AdminStoreController(getAdminStoresService, changeStoreStatusService);

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return adminPrincipal;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/admin/stores - Debe listar tiendas")
    void shouldListStores() throws Exception {
        AdminStoreSummaryResponse summary = new AdminStoreSummaryResponse(
                storeId,
                merchantId,
                "Tienda Demo",
                "tienda-demo",
                "987654321",
                StoreStatus.ACTIVE,
                true,
                true,
                Instant.now(),
                Instant.now(),
                null
        );

        when(getAdminStoresService.listStores(any(), any())).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/v1/admin/stores")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Tienda Demo"))
                .andExpect(jsonPath("$[0].slug").value("tienda-demo"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/stores/{id} - Debe obtener detalle de tienda")
    void shouldGetStoreById() throws Exception {
        AdminStoreDetailResponse detail = new AdminStoreDetailResponse(
                storeId,
                merchantId,
                "Tienda Demo",
                "tienda-demo",
                "Descripcion",
                "987654321",
                "Direccion 123",
                "Ref",
                null,
                null,
                true,
                true,
                BigDecimal.valueOf(5),
                "PEN",
                false,
                null,
                StoreStatus.ACTIVE,
                Instant.now(),
                Instant.now(),
                null
        );

        when(getAdminStoresService.getStoreById(storeId)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/admin/stores/{id}", storeId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tienda Demo"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/stores/{id}/suspend - Debe suspender tienda")
    void shouldSuspendStore() throws Exception {
        AdminStoreDetailResponse suspended = new AdminStoreDetailResponse(
                storeId,
                merchantId,
                "Tienda Demo",
                "tienda-demo",
                "Descripcion",
                "987654321",
                "Direccion 123",
                "Ref",
                null,
                null,
                true,
                true,
                BigDecimal.valueOf(5),
                "PEN",
                false,
                null,
                StoreStatus.SUSPENDED,
                Instant.now(),
                Instant.now(),
                null
        );

        when(changeStoreStatusService.suspend(eq(storeId))).thenReturn(suspended);

        mockMvc.perform(patch("/api/v1/admin/stores/{id}/suspend", storeId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/stores/{id}/activate - Debe reactivar tienda")
    void shouldActivateStore() throws Exception {
        AdminStoreDetailResponse activated = new AdminStoreDetailResponse(
                storeId,
                merchantId,
                "Tienda Demo",
                "tienda-demo",
                "Descripcion",
                "987654321",
                "Direccion 123",
                "Ref",
                null,
                null,
                true,
                true,
                BigDecimal.valueOf(5),
                "PEN",
                false,
                null,
                StoreStatus.ACTIVE,
                Instant.now(),
                Instant.now(),
                null
        );

        when(changeStoreStatusService.activate(eq(storeId))).thenReturn(activated);

        mockMvc.perform(patch("/api/v1/admin/stores/{id}/activate", storeId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}

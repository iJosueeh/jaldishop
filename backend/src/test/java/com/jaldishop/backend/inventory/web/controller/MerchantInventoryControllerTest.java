package com.jaldishop.backend.inventory.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaldishop.backend.inventory.application.service.*;
import com.jaldishop.backend.inventory.web.dto.InventoryResponse;
import com.jaldishop.backend.inventory.web.dto.UpdateStockRequest;
import com.jaldishop.backend.inventory.web.dto.UpdateThresholdRequest;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import com.jaldishop.backend.store.application.service.StoreContextService;
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

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MerchantInventoryControllerTest {

    @Mock
    private StoreContextService storeContextService;
    @Mock
    private GetStoreInventoryService getStoreInventoryService;
    @Mock
    private GetVariantInventoryService getVariantInventoryService;
    @Mock
    private GetLowStockInventoryService getLowStockInventoryService;
    @Mock
    private UpdateInventoryStockService updateInventoryStockService;
    @Mock
    private UpdateInventoryThresholdService updateInventoryThresholdService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final UUID storeId = UUID.randomUUID();
    private final UUID variantId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private JwtPrincipal principal;

    @BeforeEach
    void setUp() {
        principal = new JwtPrincipal(UUID.randomUUID(), Set.of("MERCHANT"));
        MerchantInventoryController controller = new MerchantInventoryController(
                storeContextService,
                getStoreInventoryService,
                getVariantInventoryService,
                getLowStockInventoryService,
                updateInventoryStockService,
                updateInventoryThresholdService
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter,
                                                  ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest,
                                                  WebDataBinderFactory binderFactory) {
                        return principal;
                    }
                })
                .build();
    }

    private InventoryResponse sampleResponse() {
        return new InventoryResponse(
                variantId,
                productId,
                "Café Bourbon",
                "250g",
                "CAF-BOU",
                true,
                "ACTIVE",
                "CONFIGURED",
                20,
                2,
                18,
                5,
                false,
                Instant.now()
        );
    }

    @Test
    @DisplayName("GET /api/v1/inventory retorna 200 OK con lista de inventarios")
    void testGetStoreInventory() throws Exception {
        when(storeContextService.requireStoreId(any())).thenReturn(storeId);
        when(getStoreInventoryService.execute(storeId)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].variantId").value(variantId.toString()))
                .andExpect(jsonPath("$[0].physicalQuantity").value(20))
                .andExpect(jsonPath("$[0].inventoryStatus").value("CONFIGURED"));
    }

    @Test
    @DisplayName("GET /api/v1/inventory/variants/{id} retorna 200 OK con detalle")
    void testGetVariantInventory() throws Exception {
        when(storeContextService.requireStoreId(any())).thenReturn(storeId);
        when(getVariantInventoryService.execute(storeId, variantId)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/inventory/variants/" + variantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variantId").value(variantId.toString()))
                .andExpect(jsonPath("$.availableQuantity").value(18));
    }

    @Test
    @DisplayName("GET /api/v1/inventory/low-stock retorna 200 OK")
    void testGetLowStock() throws Exception {
        when(storeContextService.requireStoreId(any())).thenReturn(storeId);
        when(getLowStockInventoryService.execute(storeId)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/inventory/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].variantId").value(variantId.toString()));
    }

    @Test
    @DisplayName("PUT /api/v1/inventory/variants/{id}/stock actualiza cantidad y retorna 200 OK")
    void testUpdateStockSuccess() throws Exception {
        when(storeContextService.requireStoreId(any())).thenReturn(storeId);
        when(updateInventoryStockService.execute(eq(storeId), eq(variantId), eq(25))).thenReturn(sampleResponse());

        UpdateStockRequest req = new UpdateStockRequest(25);

        mockMvc.perform(put("/api/v1/inventory/variants/" + variantId + "/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variantId").value(variantId.toString()));
    }

    @Test
    @DisplayName("PUT /api/v1/inventory/variants/{id}/stock retorna 400 si quantity es negativo")
    void testUpdateStockNegativeThrows() throws Exception {
        UpdateStockRequest req = new UpdateStockRequest(-5);

        mockMvc.perform(put("/api/v1/inventory/variants/" + variantId + "/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/v1/inventory/variants/{id}/threshold actualiza umbral y retorna 200 OK")
    void testUpdateThresholdSuccess() throws Exception {
        when(storeContextService.requireStoreId(any())).thenReturn(storeId);
        when(updateInventoryThresholdService.execute(eq(storeId), eq(variantId), eq(8))).thenReturn(sampleResponse());

        UpdateThresholdRequest req = new UpdateThresholdRequest(8);

        mockMvc.perform(patch("/api/v1/inventory/variants/" + variantId + "/threshold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variantId").value(variantId.toString()));
    }

    @Test
    @DisplayName("PATCH /api/v1/inventory/variants/{id}/threshold retorna 409 si inventario no está configurado")
    void testUpdateThresholdNotConfiguredThrowsConflict() throws Exception {
        when(storeContextService.requireStoreId(any())).thenReturn(storeId);
        when(updateInventoryThresholdService.execute(eq(storeId), eq(variantId), eq(8)))
                .thenThrow(new ConflictException("INVENTORY_NOT_CONFIGURED", "Sin inventario configurado"));

        UpdateThresholdRequest req = new UpdateThresholdRequest(8);

        mockMvc.perform(patch("/api/v1/inventory/variants/" + variantId + "/threshold")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }
}

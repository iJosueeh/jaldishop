package com.jaldishop.backend.catalog.web.controller;
import com.jaldishop.backend.catalog.domain.model.*;
import com.jaldishop.backend.catalog.domain.enums.*;
import com.jaldishop.backend.catalog.domain.repository.*;
import com.jaldishop.backend.catalog.application.command.*;
import com.jaldishop.backend.catalog.application.query.*;
import com.jaldishop.backend.catalog.application.service.*;
import com.jaldishop.backend.catalog.application.util.*;


import com.jaldishop.backend.catalog.application.command.CreateProductVariantCommand;
import com.jaldishop.backend.catalog.application.service.CreateProductVariantService;
import com.jaldishop.backend.catalog.application.service.GetProductVariantsService;
import com.jaldishop.backend.catalog.application.service.UpdateProductVariantService;
import com.jaldishop.backend.catalog.domain.model.ProductVariant;
import com.jaldishop.backend.catalog.web.dto.ProductVariantResponse;
import com.jaldishop.backend.catalog.web.mapper.ProductVariantResponseMapper;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.application.service.StoreContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MerchantProductVariantControllerTest {

    @Mock
    private CreateProductVariantService createVariantService;

    @Mock
    private UpdateProductVariantService updateVariantService;

    @Mock
    private GetProductVariantsService getVariantsService;

    @Mock
    private StoreContextService storeContextService;

    @Mock
    private ProductVariantResponseMapper responseMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MerchantProductVariantController controller = new MerchantProductVariantController(
                createVariantService,
                updateVariantService,
                getVariantsService,
                storeContextService,
                responseMapper
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/merchants/stores/{storeId}/products/{productId}/variants - 201 Created")
    void shouldCreateVariant() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ProductVariant variant = ProductVariant.create(
                productId,
                "Porción Individual",
                "CHEESE-IND-01",
                new BigDecimal("18.50"),
                "PEN",
                true,
                Collections.emptyList()
        );
        ProductVariantResponse response = new ProductVariantResponse(
                variant.getId(),
                productId,
                "Porción Individual",
                "CHEESE-IND-01",
                new BigDecimal("18.50"),
                "PEN",
                true,
                "ACTIVE",
                Collections.emptyList(),
                Instant.now(),
                Instant.now()
        );

        doNothing().when(storeContextService).validateStoreOwnership(any(), any());
        when(createVariantService.execute(any(CreateProductVariantCommand.class))).thenReturn(variant);
        when(responseMapper.toResponse(variant)).thenReturn(response);

        String requestJson = """
                {
                    "presentationName": "Porción Individual",
                    "sku": "CHEESE-IND-01",
                    "priceAmount": 18.50,
                    "priceCurrency": "PEN",
                    "tracksInventory": true,
                    "attributes": []
                }
                """;

        mockMvc.perform(post("/api/v1/merchants/stores/{storeId}/products/{productId}/variants", storeId, productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.presentationName").value("Porción Individual"))
                .andExpect(jsonPath("$.sku").value("CHEESE-IND-01"))
                .andExpect(jsonPath("$.priceCurrency").value("PEN"));
    }

    @Test
    @DisplayName("POST /api/v1/merchants/stores/{storeId}/products/{productId}/variants - 400 Bad Request cuando moneda no es 3 letras mayúsculas")
    void shouldRejectInvalidCurrency() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        String requestJson = """
                {
                    "presentationName": "Porción Individual",
                    "sku": "CHEESE-IND-01",
                    "priceAmount": 18.50,
                    "priceCurrency": "123",
                    "tracksInventory": true,
                    "attributes": []
                }
                """;

        mockMvc.perform(post("/api/v1/merchants/stores/{storeId}/products/{productId}/variants", storeId, productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /api/v1/merchants/stores/{storeId}/products/{productId}/variants - 200 OK")
    void shouldGetVariants() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        ProductVariant variant = ProductVariant.create(
                productId,
                "Porción Grande",
                "CHEESE-GRD-01",
                new BigDecimal("45.00"),
                "PEN",
                false,
                Collections.emptyList()
        );
        ProductVariantResponse response = new ProductVariantResponse(
                variant.getId(),
                productId,
                "Porción Grande",
                "CHEESE-GRD-01",
                new BigDecimal("45.00"),
                "PEN",
                false,
                "ACTIVE",
                Collections.emptyList(),
                Instant.now(),
                Instant.now()
        );

        doNothing().when(storeContextService).validateStoreOwnership(any(), any());
        when(getVariantsService.execute(productId, storeId)).thenReturn(List.of(variant));
        when(responseMapper.toResponse(variant)).thenReturn(response);

        mockMvc.perform(get("/api/v1/merchants/stores/{storeId}/products/{productId}/variants", storeId, productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].presentationName").value("Porción Grande"));
    }

    @Test
    @DisplayName("POST /api/v1/merchants/stores/{storeId}/products/{productId}/variants - 403 Forbidden cuando el storeId no pertenece al comerciante")
    void shouldReturn403WhenStoreNotOwnedOnCreate() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        doThrow(new AccessDeniedException("Solo los usuarios con el rol MERCHANT pueden realizar esta operación."))
                .when(storeContextService).validateStoreOwnership(eq(storeId), any());

        String requestJson = """
                {
                    "presentationName": "Porción Individual",
                    "sku": "CHEESE-IND-01",
                    "priceAmount": 18.50,
                    "priceCurrency": "PEN",
                    "tracksInventory": true,
                    "attributes": []
                }
                """;

        mockMvc.perform(post("/api/v1/merchants/stores/{storeId}/products/{productId}/variants", storeId, productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("POST /api/v1/merchants/stores/{storeId}/products/{productId}/variants - 409 Conflict cuando el SKU ya existe en la tienda")
    void shouldReturn409WhenSkuAlreadyExists() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        doNothing().when(storeContextService).validateStoreOwnership(any(), any());
        when(createVariantService.execute(any(CreateProductVariantCommand.class)))
                .thenThrow(new ConflictException("SKU_ALREADY_EXISTS", "El SKU 'CHEESE-IND-01' ya está en uso"));

        String requestJson = """
                {
                    "presentationName": "Porción Individual",
                    "sku": "CHEESE-IND-01",
                    "priceAmount": 18.50,
                    "priceCurrency": "PEN",
                    "tracksInventory": true,
                    "attributes": []
                }
                """;

        mockMvc.perform(post("/api/v1/merchants/stores/{storeId}/products/{productId}/variants", storeId, productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SKU_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("El SKU 'CHEESE-IND-01' ya está en uso"));
    }

    @Test
    @DisplayName("GET /api/v1/merchants/stores/{storeId}/products/{productId}/variants/{variantId} - 404 Not Found cuando la variante no existe")
    void shouldReturn404WhenVariantNotFound() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        doNothing().when(storeContextService).validateStoreOwnership(any(), any());
        when(getVariantsService.execute(variantId, productId, storeId))
                .thenThrow(new ResourceNotFoundException("Variante no encontrada"));

        mockMvc.perform(get("/api/v1/merchants/stores/{storeId}/products/{productId}/variants/{variantId}", storeId, productId, variantId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Variante no encontrada"));
    }

    @Test
    void rejectsInvalidAttributesBeforeEitherServiceIsCalled() throws Exception {
        String[] invalidAttributes = {
                "[null]", "[{\"name\":null,\"value\":\"v\"}]",
                "[{\"name\":\"  \",\"value\":\"v\"}]",
                "[{\"name\":\"n\",\"value\":null}]",
                "[{\"name\":\"n\",\"value\":\"  \"}]",
                "[{\"name\":\"" + "n".repeat(121) + "\",\"value\":\"v\"}]"
        };
        String base = "/api/v1/merchants/stores/" + UUID.randomUUID() + "/products/" + UUID.randomUUID() + "/variants";
        for (String attributes : invalidAttributes) {
            String body = variantRequest(attributes);
            mockMvc.perform(post(base).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.errors").isMap());
            mockMvc.perform(put(base + "/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.errors").isMap());
        }
        verifyNoInteractions(createVariantService, updateVariantService, storeContextService);
    }

    @Test
    void rejectsDuplicateNormalizedAttributeNamesBeforePersistence() throws Exception {
        String base = "/api/v1/merchants/stores/" + UUID.randomUUID() + "/products/" + UUID.randomUUID() + "/variants";
        String body = variantRequest("[{\"name\":\"Color\",\"value\":\"azul\"},{\"name\":\" Color \",\"value\":\"rojo\"}]");
        mockMvc.perform(post(base).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        mockMvc.perform(put(base + "/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        verifyNoInteractions(createVariantService, updateVariantService);
    }

    @Test
    void acceptsAttributeNameAtLimitAndEmptyOrNullListsForCreateAndUpdate() throws Exception {
        UUID productId = UUID.randomUUID();
        String base = "/api/v1/merchants/stores/" + UUID.randomUUID() + "/products/" + productId + "/variants";
        ProductVariant variant = ProductVariant.create(productId, "Normal", null, new BigDecimal("1.00"), "PEN", false, List.of());
        when(createVariantService.execute(any())).thenReturn(variant);
        when(updateVariantService.execute(any())).thenReturn(variant);
        when(responseMapper.toResponse(variant)).thenReturn(new ProductVariantResponse(
                variant.getId(), productId, "Normal", null, new BigDecimal("1.00"), "PEN", false,
                "ACTIVE", List.of(), Instant.now(), Instant.now()));
        for (String attributes : List.of("[]", "null", "[{\"name\":\"" + "n".repeat(120) + "\",\"value\":\"" + "v".repeat(300) + "\"}]")) {
            String body = variantRequest(attributes);
            mockMvc.perform(post(base).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
            mockMvc.perform(put(base + "/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
        }
    }

    private String variantRequest(String attributes) {
        return "{\"presentationName\":\"Normal\",\"priceAmount\":1,\"priceCurrency\":\"PEN\",\"tracksInventory\":false,\"attributes\":" + attributes + "}";
    }
}

package com.jaldishop.backend.capacity.web.controller;

import com.jaldishop.backend.capacity.application.occupancy.service.GetMerchantStoreCapacityOccupancyService;
import com.jaldishop.backend.capacity.web.dto.MerchantCapacitySlotOccupancyResponse;
import com.jaldishop.backend.capacity.web.dto.MerchantStoreCapacityOccupancyResponse;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MerchantCapacityController — Pruebas web de controlador y seguridad")
class MerchantCapacityControllerTest {

    @Mock
    private StoreContextService storeContextService;

    @Mock
    private GetMerchantStoreCapacityOccupancyService occupancyService;

    private MockMvc mockMvc;
    private UUID testMerchantId;
    private UUID testStoreId;
    private JwtPrincipal currentPrincipal;

    @BeforeEach
    void setUp() {
        testMerchantId = UUID.randomUUID();
        testStoreId = UUID.randomUUID();
        currentPrincipal = new JwtPrincipal(testMerchantId, Set.of("MERCHANT"));

        MerchantCapacityController controller = new MerchantCapacityController(storeContextService, occupancyService);

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
                        return currentPrincipal;
                    }
                })
                .build();
    }

    @Test
    @DisplayName("GET /occupancy retorna 200 OK con datos de ocupación para comerciante legítimo")
    void testGetOccupancy_Success() throws Exception {
        LocalDate date = LocalDate.of(2026, 11, 16);
        MerchantCapacitySlotOccupancyResponse slot = new MerchantCapacitySlotOccupancyResponse(
                LocalTime.of(9, 0), LocalTime.of(13, 0), 10, 4, 6, 1, 3, 40
        );
        MerchantStoreCapacityOccupancyResponse response = new MerchantStoreCapacityOccupancyResponse(
                testStoreId, date, false, 10, 4, 6, 1, 3, 40, List.of(slot)
        );

        when(occupancyService.execute(eq(testStoreId), eq(date))).thenReturn(response);

        mockMvc.perform(get("/api/v1/merchants/stores/{storeId}/capacity/occupancy", testStoreId)
                        .param("date", "2026-11-16")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeId").value(testStoreId.toString()))
                .andExpect(jsonPath("$.date").value("2026-11-16"))
                .andExpect(jsonPath("$.isClosed").value(false))
                .andExpect(jsonPath("$.effectiveCapacity").value(10))
                .andExpect(jsonPath("$.occupiedCapacity").value(4))
                .andExpect(jsonPath("$.availableCapacity").value(6))
                .andExpect(jsonPath("$.reservedCapacity").value(1))
                .andExpect(jsonPath("$.committedCapacity").value(3))
                .andExpect(jsonPath("$.occupancyPercentage").value(40))
                .andExpect(jsonPath("$.slots[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$.slots[0].endTime").value("13:00:00"));

        verify(storeContextService).validateStoreOwnership(testStoreId, currentPrincipal);
    }

    @Test
    @DisplayName("GET /occupancy sin parámetro date ejecuta con fecha null (delegando resolución a zona horaria)")
    void testGetOccupancy_WithoutDateParam() throws Exception {
        MerchantStoreCapacityOccupancyResponse response = new MerchantStoreCapacityOccupancyResponse(
                testStoreId, LocalDate.now(), false, 20, 5, 15, 1, 4, 25, List.of()
        );

        when(occupancyService.execute(eq(testStoreId), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/merchants/stores/{storeId}/capacity/occupancy", testStoreId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeId").value(testStoreId.toString()));

        verify(storeContextService).validateStoreOwnership(testStoreId, currentPrincipal);
    }

    @Test
    @DisplayName("GET /occupancy retorna 403 Forbidden cuando validateStoreOwnership rechaza el acceso")
    void testGetOccupancy_ForbiddenWhenStoreNotOwned() throws Exception {
        UUID otherStoreId = UUID.randomUUID();
        doThrow(new AccessDeniedException("No tiene permisos para operar sobre una tienda ajena."))
                .when(storeContextService).validateStoreOwnership(eq(otherStoreId), eq(currentPrincipal));

        mockMvc.perform(get("/api/v1/merchants/stores/{storeId}/capacity/occupancy", otherStoreId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /occupancy retorna 401 Unauthorized cuando no existe principal autenticado")
    void testGetOccupancy_UnauthorizedWhenPrincipalIsNull() throws Exception {
        currentPrincipal = null;

        mockMvc.perform(get("/api/v1/merchants/stores/{storeId}/capacity/occupancy", testStoreId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}

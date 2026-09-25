package com.jaldishop.backend.capacity.web.controller;

import com.jaldishop.backend.capacity.application.EffectiveCapacityQuery;
import com.jaldishop.backend.capacity.application.EffectiveCapacityResult;
import com.jaldishop.backend.capacity.application.EffectiveCapacitySource;
import com.jaldishop.backend.capacity.application.GetEffectiveCapacityService;
import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
import com.jaldishop.backend.store.application.StoreContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EffectiveCapacityControllerTest {

    @Mock
    private StoreContextService storeContextService;
    @Mock
    private GetEffectiveCapacityService getEffectiveCapacityService;

    private MockMvc mockMvc;
    private UUID testMerchantId;
    private UUID testStoreId;
    private JwtPrincipal currentPrincipal;

    @BeforeEach
    void setUp() {
        testMerchantId = UUID.randomUUID();
        testStoreId = UUID.randomUUID();
        currentPrincipal = new JwtPrincipal(testMerchantId, Set.of("MERCHANT"));

        EffectiveCapacityController controller = new EffectiveCapacityController(
                storeContextService, getEffectiveCapacityService);

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
    @DisplayName("GET /api/v1/capacity/effective - Capacidad base (200)")
    void effectiveCapacityFromBase() throws Exception {
        when(storeContextService.requireStoreId(any())).thenReturn(testStoreId);
        when(getEffectiveCapacityService.execute(any(EffectiveCapacityQuery.class)))
                .thenReturn(new EffectiveCapacityResult(20, EffectiveCapacitySource.BASE));

        mockMvc.perform(get("/api/v1/capacity/effective")
                        .param("date", "2026-09-22")
                        .param("startTime", "10:00")
                        .param("endTime", "12:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeId").value(testStoreId.toString()))
                .andExpect(jsonPath("$.serviceDate").value("2026-09-22"))
                .andExpect(jsonPath("$.startTime").value("10:00:00"))
                .andExpect(jsonPath("$.endTime").value("12:00:00"))
                .andExpect(jsonPath("$.effectiveCapacity").value(20))
                .andExpect(jsonPath("$.source").value("BASE"));
    }

    @Test
    @DisplayName("GET /api/v1/capacity/effective - Capacidad desde excepción (200)")
    void effectiveCapacityFromException() throws Exception {
        when(storeContextService.requireStoreId(any())).thenReturn(testStoreId);
        when(getEffectiveCapacityService.execute(any(EffectiveCapacityQuery.class)))
                .thenReturn(new EffectiveCapacityResult(4, EffectiveCapacitySource.EXCEPTION));

        mockMvc.perform(get("/api/v1/capacity/effective")
                        .param("date", "2026-09-22")
                        .param("startTime", "12:30")
                        .param("endTime", "13:30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.effectiveCapacity").value(4))
                .andExpect(jsonPath("$.source").value("EXCEPTION"));
    }

    @Test
    @DisplayName("GET /api/v1/capacity/effective - Sin cobertura (200, 0 y NONE)")
    void effectiveCapacityNone() throws Exception {
        when(storeContextService.requireStoreId(any())).thenReturn(testStoreId);
        when(getEffectiveCapacityService.execute(any(EffectiveCapacityQuery.class)))
                .thenReturn(new EffectiveCapacityResult(0, EffectiveCapacitySource.NONE));

        mockMvc.perform(get("/api/v1/capacity/effective")
                        .param("date", "2026-09-22")
                        .param("startTime", "18:00")
                        .param("endTime", "19:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.effectiveCapacity").value(0))
                .andExpect(jsonPath("$.source").value("NONE"));
    }

    @Test
    @DisplayName("GET sin rol MERCHANT - 403 Forbidden")
    void forbiddenWhenNotMerchant() throws Exception {
        when(storeContextService.requireStoreId(any()))
                .thenThrow(new AccessDeniedException("Solo los usuarios con el rol MERCHANT pueden realizar esta operación."));

        mockMvc.perform(get("/api/v1/capacity/effective")
                        .param("date", "2026-09-22")
                        .param("startTime", "10:00")
                        .param("endTime", "12:00"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET con rango horario inválido - 422 Unprocessable Entity")
    void invalidTimeRange() throws Exception {
        when(storeContextService.requireStoreId(any())).thenReturn(testStoreId);
        when(getEffectiveCapacityService.execute(any(EffectiveCapacityQuery.class)))
                .thenThrow(new BusinessRuleException("INVALID_CAPACITY_QUERY", "startTime debe ser anterior a endTime."));

        mockMvc.perform(get("/api/v1/capacity/effective")
                        .param("date", "2026-09-22")
                        .param("startTime", "12:00")
                        .param("endTime", "10:00"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_CAPACITY_QUERY"));
    }

    @Test
    @DisplayName("GET sin franja completa - 422 Unprocessable Entity")
    void missingFranja() throws Exception {
        when(storeContextService.requireStoreId(any())).thenReturn(testStoreId);
        when(getEffectiveCapacityService.execute(any(EffectiveCapacityQuery.class)))
                .thenThrow(new BusinessRuleException("INVALID_CAPACITY_QUERY", "Debe indicar la franja horaria (startTime y endTime)."));

        mockMvc.perform(get("/api/v1/capacity/effective")
                        .param("date", "2026-09-22"))
                .andExpect(status().isUnprocessableEntity());
    }
}
package com.jaldishop.backend.capacity.web.controller;

import com.jaldishop.backend.capacity.application.availability.CapacityAvailabilityResult;
import com.jaldishop.backend.capacity.application.availability.CapacitySlotAvailability;
import com.jaldishop.backend.capacity.application.availability.ListStoreCapacitySlotsService;
import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CapacitySlotsControllerTest {

    @Mock
    private ListStoreCapacitySlotsService listService;

    private MockMvc mockMvc;
    private UUID testCustomerId;
    private UUID testStoreId;
    private JwtPrincipal currentPrincipal;
    private LocalDate futureDate;

    @BeforeEach
    void setUp() {
        testCustomerId = UUID.randomUUID();
        testStoreId = UUID.randomUUID();
        currentPrincipal = new JwtPrincipal(testCustomerId, Set.of("CUSTOMER"));
        futureDate = LocalDate.now().plusDays(7);

        CapacitySlotsController controller = new CapacitySlotsController(listService);

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
    @DisplayName("GET /api/v1/capacity/slots - Franjas con disponibilidad (200)")
    void listSlots() throws Exception {
        when(listService.execute(eq(testStoreId), eq(futureDate))).thenReturn(List.of(
                new CapacitySlotAvailability(LocalTime.of(9, 0), LocalTime.of(12, 0),
                        new CapacityAvailabilityResult(10, 4, 4, 2)),
                new CapacitySlotAvailability(LocalTime.of(14, 0), LocalTime.of(18, 0),
                        new CapacityAvailabilityResult(8, 0, 5, 3))));

        mockMvc.perform(get("/api/v1/capacity/slots")
                        .param("storeId", testStoreId.toString())
                        .param("date", futureDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeId").value(testStoreId.toString()))
                .andExpect(jsonPath("$.serviceDate").value(futureDate.toString()))
                .andExpect(jsonPath("$.slots.length()").value(2))
                .andExpect(jsonPath("$.slots[0].storeId").value(testStoreId.toString()))
                .andExpect(jsonPath("$.slots[0].serviceDate").value(futureDate.toString()))
                .andExpect(jsonPath("$.slots[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$.slots[0].endTime").value("12:00:00"))
                .andExpect(jsonPath("$.slots[0].effectiveCapacity").value(10))
                .andExpect(jsonPath("$.slots[0].availableCapacity").value(4))
                .andExpect(jsonPath("$.slots[0].reservedCapacity").value(4))
                .andExpect(jsonPath("$.slots[0].committedCapacity").value(2))
                // occupiedCapacity = reservas activas + comprometidas; nunca solo reserved.
                .andExpect(jsonPath("$.slots[0].occupiedCapacity").value(6))
                .andExpect(jsonPath("$.slots[1].availableCapacity").value(0))
                .andExpect(jsonPath("$.slots[1].occupiedCapacity").value(8));
    }

    @Test
    @DisplayName("GET /api/v1/capacity/slots - Sin candidatas devuelve lista vacía (200)")
    void listSlotsEmpty() throws Exception {
        when(listService.execute(eq(testStoreId), eq(futureDate))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/capacity/slots")
                        .param("storeId", testStoreId.toString())
                        .param("date", futureDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots").isArray())
                .andExpect(jsonPath("$.slots.length()").value(0));
    }

    @Test
    @DisplayName("Sin rol CUSTOMER - 403 Forbidden")
    void forbiddenWhenNotCustomer() throws Exception {
        currentPrincipal = new JwtPrincipal(testCustomerId, Set.of("MERCHANT"));

        mockMvc.perform(get("/api/v1/capacity/slots")
                        .param("storeId", testStoreId.toString())
                        .param("date", futureDate.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Sin principal autenticado - 403 Forbidden")
    void forbiddenWhenNoPrincipal() throws Exception {
        currentPrincipal = null;

        mockMvc.perform(get("/api/v1/capacity/slots")
                        .param("storeId", testStoreId.toString())
                        .param("date", futureDate.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Sin storeId - 422 INVALID_CAPACITY_QUERY")
    void missingStoreId() throws Exception {
        mockMvc.perform(get("/api/v1/capacity/slots")
                        .param("date", futureDate.toString()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_CAPACITY_QUERY"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("Sin date - 422 INVALID_CAPACITY_QUERY")
    void missingDate() throws Exception {
        mockMvc.perform(get("/api/v1/capacity/slots")
                        .param("storeId", testStoreId.toString()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_CAPACITY_QUERY"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("storeId mal formado - 422 INVALID_CAPACITY_QUERY")
    void malformedStoreId() throws Exception {
        mockMvc.perform(get("/api/v1/capacity/slots")
                        .param("storeId", "no-es-uuid")
                        .param("date", futureDate.toString()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_CAPACITY_QUERY"));
    }

    @Test
    @DisplayName("date mal formada - 422 INVALID_CAPACITY_QUERY")
    void malformedDate() throws Exception {
        mockMvc.perform(get("/api/v1/capacity/slots")
                        .param("storeId", testStoreId.toString())
                        .param("date", "22-09-2026"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_CAPACITY_QUERY"));
    }

    @Test
    @DisplayName("Fecha pasada - 422 INVALID_CAPACITY_QUERY (422 propagado desde el servicio)")
    void pastDateRejected() throws Exception {
        LocalDate pastDate = LocalDate.now().minusDays(1);
        when(listService.execute(any(), eq(pastDate)))
                .thenThrow(new BusinessRuleException("INVALID_CAPACITY_QUERY",
                        "No es posible consultar franjas de fechas pasadas."));

        mockMvc.perform(get("/api/v1/capacity/slots")
                        .param("storeId", testStoreId.toString())
                        .param("date", pastDate.toString()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_CAPACITY_QUERY"))
                .andExpect(jsonPath("$.message")
                        .value("No es posible consultar franjas de fechas pasadas."));
    }
}

package com.jaldishop.backend.capacity.web.controller;

import com.jaldishop.backend.capacity.application.CapacityAvailabilityResult;
import com.jaldishop.backend.capacity.application.CreateCapacityReservationService;
import com.jaldishop.backend.capacity.application.GetCapacityAvailabilityService;
import com.jaldishop.backend.capacity.application.GetCapacityReservationService;
import com.jaldishop.backend.capacity.application.ReleaseCapacityReservationService;
import com.jaldishop.backend.capacity.domain.CapacityReservation;
import com.jaldishop.backend.capacity.domain.CapacityReservationStatus;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CapacityReservationControllerTest {

    @Mock
    private GetCapacityAvailabilityService availabilityService;
    @Mock
    private CreateCapacityReservationService createService;
    @Mock
    private GetCapacityReservationService getService;
    @Mock
    private ReleaseCapacityReservationService releaseService;

    private MockMvc mockMvc;
    private UUID testCustomerId;
    private UUID testStoreId;
    private JwtPrincipal currentPrincipal;

    @BeforeEach
    void setUp() {
        testCustomerId = UUID.randomUUID();
        testStoreId = UUID.randomUUID();
        currentPrincipal = new JwtPrincipal(testCustomerId, Set.of("CUSTOMER"));

        CapacityReservationController controller = new CapacityReservationController(
                availabilityService, createService, getService, releaseService);

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
    @DisplayName("GET /api/v1/capacity/availability - Consultar disponibilidad (200)")
    void availability() throws Exception {
        when(availabilityService.execute(any()))
                .thenReturn(new CapacityAvailabilityResult(20, 14, 4, 2));

        mockMvc.perform(get("/api/v1/capacity/availability")
                        .param("storeId", testStoreId.toString())
                        .param("date", "2026-09-22")
                        .param("startTime", "10:00")
                        .param("endTime", "12:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeId").value(testStoreId.toString()))
                .andExpect(jsonPath("$.serviceDate").value("2026-09-22"))
                .andExpect(jsonPath("$.startTime").value("10:00:00"))
                .andExpect(jsonPath("$.endTime").value("12:00:00"))
                .andExpect(jsonPath("$.effectiveCapacity").value(20))
                .andExpect(jsonPath("$.availableCapacity").value(14))
                .andExpect(jsonPath("$.reservedCapacity").value(4))
                .andExpect(jsonPath("$.committedCapacity").value(2));
    }

    @Test
    @DisplayName("POST /api/v1/capacity/reservations - Crear reserva (201)")
    void createReservation() throws Exception {
        CapacityReservation reservation = CapacityReservation.create(
                testStoreId, testCustomerId, LocalDate.of(2026, 9, 22),
                LocalTime.of(10, 0), LocalTime.of(12, 0));
        when(createService.execute(any())).thenReturn(reservation);

        mockMvc.perform(post("/api/v1/capacity/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "serviceDate": "2026-09-22",
                                  "startTime": "10:00:00",
                                  "endTime": "12:00:00"
                                }
                                """.formatted(testStoreId.toString())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.storeId").value(testStoreId.toString()))
                .andExpect(jsonPath("$.userId").value(testCustomerId.toString()))
                .andExpect(jsonPath("$.serviceDate").value("2026-09-22"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/capacity/reservations - Body inválido (400)")
    void createReservationInvalidBody() throws Exception {
        mockMvc.perform(post("/api/v1/capacity/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceDate": "2026-09-22"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/capacity/reservations - Franja saturada (409 CAPACITY_EXHAUSTED)")
    void createReservationExhausted() throws Exception {
        when(createService.execute(any()))
                .thenThrow(new ConflictException("CAPACITY_EXHAUSTED", "No quedan cupos disponibles para la franja horaria seleccionada."));

        mockMvc.perform(post("/api/v1/capacity/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "serviceDate": "2026-09-22",
                                  "startTime": "10:00:00",
                                  "endTime": "12:00:00"
                                }
                                """.formatted(testStoreId.toString())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CAPACITY_EXHAUSTED"));
    }

    @Test
    @DisplayName("GET /api/v1/capacity/reservations/{id} - Obtener reserva propia (200)")
    void getReservation() throws Exception {
        UUID id = UUID.randomUUID();
        CapacityReservation reservation = reconstituted(id, CapacityReservationStatus.ACTIVE);
        when(getService.execute(eq(id), eq(testCustomerId))).thenReturn(reservation);

        mockMvc.perform(get("/api/v1/capacity/reservations/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/v1/capacity/reservations/{id} - Reserva ajena (404)")
    void getForeignReservation() throws Exception {
        UUID id = UUID.randomUUID();
        when(getService.execute(eq(id), eq(testCustomerId)))
                .thenThrow(new ResourceNotFoundException("reserva de capacidad", id));

        mockMvc.perform(get("/api/v1/capacity/reservations/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("DELETE /api/v1/capacity/reservations/{id} - Liberar reserva (200)")
    void releaseReservation() throws Exception {
        UUID id = UUID.randomUUID();
        CapacityReservation reservation = reconstituted(id, CapacityReservationStatus.ACTIVE);
        reservation.release();
        when(releaseService.execute(eq(id), eq(testCustomerId))).thenReturn(reservation);

        mockMvc.perform(delete("/api/v1/capacity/reservations/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("RELEASED"));
    }

    @Test
    @DisplayName("Sin rol CUSTOMER - 403 Forbidden")
    void forbiddenWhenNotCustomer() throws Exception {
        currentPrincipal = new JwtPrincipal(testCustomerId, Set.of("MERCHANT"));

        mockMvc.perform(get("/api/v1/capacity/availability")
                        .param("storeId", testStoreId.toString())
                        .param("date", "2026-09-22")
                        .param("startTime", "10:00")
                        .param("endTime", "12:00"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Sin rol CUSTOMER en creación - 403 Forbidden")
    void forbiddenWhenNotCustomerOnCreate() throws Exception {
        currentPrincipal = new JwtPrincipal(testCustomerId, Set.of("MERCHANT"));

        mockMvc.perform(post("/api/v1/capacity/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storeId": "%s",
                                  "serviceDate": "2026-09-22",
                                  "startTime": "10:00:00",
                                  "endTime": "12:00:00"
                                }
                                """.formatted(testStoreId.toString())))
                .andExpect(status().isForbidden());
    }

    private CapacityReservation reconstituted(UUID id, CapacityReservationStatus status) {
        Instant now = Instant.now();
        Instant expiresAt = status == CapacityReservationStatus.ACTIVE
                ? now.plusSeconds(600)
                : now.minusSeconds(60);
        return CapacityReservation.reconstitute(
                id, testStoreId, testCustomerId, LocalDate.of(2026, 9, 22),
                LocalTime.of(10, 0), LocalTime.of(12, 0), status,
                expiresAt, null, now, now);
    }
}
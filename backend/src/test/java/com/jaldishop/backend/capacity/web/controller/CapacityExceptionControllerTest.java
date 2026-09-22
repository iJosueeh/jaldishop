package com.jaldishop.backend.capacity.web.controller;

import com.jaldishop.backend.capacity.application.*;
import com.jaldishop.backend.capacity.domain.CapacityException;
import com.jaldishop.backend.capacity.domain.CapacityExceptionStatus;
import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
import com.jaldishop.backend.store.application.StoreContextService;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CapacityExceptionControllerTest {

    @Mock
    private StoreContextService storeContextService;
    @Mock
    private CreateCapacityExceptionService createService;
    @Mock
    private GetStoreCapacityExceptionsService listService;
    @Mock
    private UpdateCapacityExceptionService updateService;
    @Mock
    private ToggleCapacityExceptionStatusService toggleService;

    private MockMvc mockMvc;
    private UUID testMerchantId;
    private UUID testStoreId;
    private JwtPrincipal currentPrincipal;

    @BeforeEach
    void setUp() {
        testMerchantId = UUID.randomUUID();
        testStoreId = UUID.randomUUID();
        currentPrincipal = new JwtPrincipal(testMerchantId, Set.of("MERCHANT"));

        CapacityExceptionController controller = new CapacityExceptionController(
                storeContextService, createService, listService, updateService, toggleService);

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
    @DisplayName("POST /api/v1/capacity-exceptions - Crear exitosamente (201)")
    void createSuccess() throws Exception {
        CapacityException exception = CapacityException.create(
                testStoreId, LocalDate.of(2026, 12, 25), null, null, 0, "Navidad");

        when(storeContextService.requireStoreId(any())).thenReturn(testStoreId);
        when(createService.execute(any(CreateCapacityExceptionCommand.class))).thenReturn(exception);

        String json = """
                {"serviceDate": "2026-12-25", "exceptionCapacity": 0, "reason": "Navidad"}
                """;

        mockMvc.perform(post("/api/v1/capacity-exceptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serviceDate").value("2026-12-25"))
                .andExpect(jsonPath("$.exceptionCapacity").value(0))
                .andExpect(jsonPath("$.reason").value("Navidad"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/v1/capacity-exceptions - Listar excepciones")
    void listSuccess() throws Exception {
        List<CapacityException> exceptions = List.of(
                CapacityException.create(testStoreId, LocalDate.of(2026, 12, 25), null, null, 0, "Navidad"),
                CapacityException.create(testStoreId, LocalDate.of(2026, 7, 15), LocalTime.of(10, 0), LocalTime.of(14, 0), 5, null));

        when(storeContextService.requireStoreId(any())).thenReturn(testStoreId);
        when(listService.execute(testStoreId)).thenReturn(exceptions);

        mockMvc.perform(get("/api/v1/capacity-exceptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceDate").value("2026-12-25"))
                .andExpect(jsonPath("$[1].serviceDate").value("2026-07-15"));
    }

    @Test
    @DisplayName("PUT /api/v1/capacity-exceptions/{id} - Actualizar exitosamente")
    void updateSuccess() throws Exception {
        UUID exceptionId = UUID.randomUUID();
        CapacityException exception = CapacityException.reconstitute(
                exceptionId, testStoreId, LocalDate.of(2026, 6, 20),
                LocalTime.of(8, 0), LocalTime.of(20, 0), 15, "Verano",
                CapacityExceptionStatus.ACTIVE, Instant.now(), Instant.now());

        when(storeContextService.requireStoreId(any())).thenReturn(testStoreId);
        when(updateService.execute(any(UpdateCapacityExceptionCommand.class))).thenReturn(exception);

        String json = """
                {"serviceDate": "2026-06-20", "startTime": "08:00", "endTime": "20:00", "exceptionCapacity": 15, "reason": "Verano"}
                """;

        mockMvc.perform(put("/api/v1/capacity-exceptions/" + exceptionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceDate").value("2026-06-20"))
                .andExpect(jsonPath("$.exceptionCapacity").value(15));
    }

    @Test
    @DisplayName("PATCH /api/v1/capacity-exceptions/{id}/activate - Activar")
    void activateSuccess() throws Exception {
        UUID exceptionId = UUID.randomUUID();
        CapacityException exception = CapacityException.reconstitute(
                exceptionId, testStoreId, LocalDate.of(2026, 1, 1),
                null, null, 0, null,
                CapacityExceptionStatus.ACTIVE, Instant.now(), Instant.now());

        when(storeContextService.requireStoreId(any())).thenReturn(testStoreId);
        when(toggleService.activate(exceptionId, testStoreId)).thenReturn(exception);

        mockMvc.perform(patch("/api/v1/capacity-exceptions/" + exceptionId + "/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("PATCH /api/v1/capacity-exceptions/{id}/deactivate - Desactivar")
    void deactivateSuccess() throws Exception {
        UUID exceptionId = UUID.randomUUID();
        CapacityException exception = CapacityException.reconstitute(
                exceptionId, testStoreId, LocalDate.of(2026, 1, 1),
                null, null, 0, null,
                CapacityExceptionStatus.INACTIVE, Instant.now(), Instant.now());

        when(storeContextService.requireStoreId(any())).thenReturn(testStoreId);
        when(toggleService.deactivate(exceptionId, testStoreId)).thenReturn(exception);

        mockMvc.perform(patch("/api/v1/capacity-exceptions/" + exceptionId + "/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    @DisplayName("POST sin rol MERCHANT - 403 Forbidden")
    void createForbiddenWhenNotMerchant() throws Exception {
        when(storeContextService.requireStoreId(any()))
                .thenThrow(new AccessDeniedException("Solo los usuarios con el rol MERCHANT pueden realizar esta operación."));

        String json = """
                {"serviceDate": "2026-12-25", "exceptionCapacity": 0}
                """;

        mockMvc.perform(post("/api/v1/capacity-exceptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }
}
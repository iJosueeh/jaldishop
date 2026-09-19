package com.jaldishop.backend.capacity.web.controller;

import com.jaldishop.backend.capacity.application.*;
import com.jaldishop.backend.capacity.domain.CapacityConfiguration;
import com.jaldishop.backend.capacity.domain.CapacityConfigurationStatus;
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
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CapacityConfigurationControllerTest {

    @Mock
    private StoreContextService storeContextService;
    @Mock
    private CreateCapacityConfigurationService createService;
    @Mock
    private GetStoreCapacityConfigurationsService listService;
    @Mock
    private UpdateCapacityConfigurationService updateService;
    @Mock
    private ToggleCapacityConfigurationStatusService toggleService;

    private MockMvc mockMvc;
    private UUID testMerchantId;
    private UUID testStoreId;
    private JwtPrincipal currentPrincipal;

    @BeforeEach
    void setUp() {
        testMerchantId = UUID.randomUUID();
        testStoreId = UUID.randomUUID();
        currentPrincipal = new JwtPrincipal(testMerchantId, Set.of("MERCHANT"));

        CapacityConfigurationController controller = new CapacityConfigurationController(
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
    @DisplayName("POST /api/v1/capacity-configurations - Crear exitosamente (201)")
    void createSuccess() throws Exception {
        CapacityConfiguration config = CapacityConfiguration.create(
                testStoreId, 1, LocalTime.of(9, 0), LocalTime.of(17, 0), 15);

        when(storeContextService.requireStoreId(any())).thenReturn(testStoreId);
        when(createService.execute(any(CreateCapacityConfigurationCommand.class))).thenReturn(config);

        String json = """
                {"dayOfWeek": 1, "startTime": "09:00", "endTime": "17:00", "maxCapacity": 15}
                """;

        mockMvc.perform(post("/api/v1/capacity-configurations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dayOfWeek").value(1))
                .andExpect(jsonPath("$.maxCapacity").value(15))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/v1/capacity-configurations - Listar configuraciones")
    void listSuccess() throws Exception {
        List<CapacityConfiguration> configs = List.of(
                CapacityConfiguration.create(testStoreId, 0, null, null, 10),
                CapacityConfiguration.create(testStoreId, 1, LocalTime.of(9, 0), LocalTime.of(17, 0), 20));

        when(storeContextService.requireStoreId(any())).thenReturn(testStoreId);
        when(listService.execute(testStoreId)).thenReturn(configs);

        mockMvc.perform(get("/api/v1/capacity-configurations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dayOfWeek").value(0))
                .andExpect(jsonPath("$[1].dayOfWeek").value(1));
    }

    @Test
    @DisplayName("PUT /api/v1/capacity-configurations/{id} - Actualizar exitosamente")
    void updateSuccess() throws Exception {
        UUID configId = UUID.randomUUID();
        CapacityConfiguration config = CapacityConfiguration.reconstitute(
                configId, testStoreId, 3, LocalTime.of(10, 0), LocalTime.of(14, 0), 25,
                CapacityConfigurationStatus.ACTIVE, Instant.now(), Instant.now());

        when(storeContextService.requireStoreId(any())).thenReturn(testStoreId);
        when(updateService.execute(any(UpdateCapacityConfigurationCommand.class))).thenReturn(config);

        String json = """
                {"dayOfWeek": 3, "startTime": "10:00", "endTime": "14:00", "maxCapacity": 25}
                """;

        mockMvc.perform(put("/api/v1/capacity-configurations/" + configId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dayOfWeek").value(3))
                .andExpect(jsonPath("$.maxCapacity").value(25));
    }

    @Test
    @DisplayName("PATCH /api/v1/capacity-configurations/{id}/activate - Activar")
    void activateSuccess() throws Exception {
        UUID configId = UUID.randomUUID();
        CapacityConfiguration config = CapacityConfiguration.reconstitute(
                configId, testStoreId, 0, null, null, 10,
                CapacityConfigurationStatus.ACTIVE, Instant.now(), Instant.now());

        when(storeContextService.requireStoreId(any())).thenReturn(testStoreId);
        when(toggleService.activate(configId, testStoreId)).thenReturn(config);

        mockMvc.perform(patch("/api/v1/capacity-configurations/" + configId + "/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("PATCH /api/v1/capacity-configurations/{id}/deactivate - Desactivar")
    void deactivateSuccess() throws Exception {
        UUID configId = UUID.randomUUID();
        CapacityConfiguration config = CapacityConfiguration.reconstitute(
                configId, testStoreId, 0, null, null, 10,
                CapacityConfigurationStatus.INACTIVE, Instant.now(), Instant.now());

        when(storeContextService.requireStoreId(any())).thenReturn(testStoreId);
        when(toggleService.deactivate(configId, testStoreId)).thenReturn(config);

        mockMvc.perform(patch("/api/v1/capacity-configurations/" + configId + "/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    @DisplayName("POST sin rol MERCHANT - 403 Forbidden")
    void createForbiddenWhenNotMerchant() throws Exception {
        when(storeContextService.requireStoreId(any()))
                .thenThrow(new AccessDeniedException("Solo los usuarios con el rol MERCHANT pueden realizar esta operación."));

        String json = """
                {"dayOfWeek": 1, "maxCapacity": 10}
                """;

        mockMvc.perform(post("/api/v1/capacity-configurations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden());
    }
}

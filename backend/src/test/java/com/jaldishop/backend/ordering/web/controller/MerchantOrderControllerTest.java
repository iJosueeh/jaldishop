package com.jaldishop.backend.ordering.web.controller;

import com.jaldishop.backend.ordering.application.service.ChangeOrderStatusService;
import com.jaldishop.backend.ordering.application.service.CreateMerchantOrderService;
import com.jaldishop.backend.ordering.application.service.GetMerchantOrderDetailService;
import com.jaldishop.backend.ordering.application.service.GetMerchantOrderStatusCountsService;
import com.jaldishop.backend.ordering.application.service.ListMerchantOrdersService;
import com.jaldishop.backend.ordering.application.result.MerchantOrderDetailResponse;
import com.jaldishop.backend.ordering.application.result.MerchantOrderItemResponse;
import com.jaldishop.backend.ordering.application.result.MerchantOrderPageResponse;
import com.jaldishop.backend.ordering.application.result.MerchantOrderStatusCountsResponse;
import com.jaldishop.backend.ordering.application.result.MerchantOrderSummaryResponse;
import com.jaldishop.backend.ordering.domain.enums.DeliveryMode;
import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import com.jaldishop.backend.store.application.service.StoreContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MerchantOrderControllerTest {

    @Mock
    private StoreContextService storeContextService;

    @Mock
    private ListMerchantOrdersService listMerchantOrdersService;

    @Mock
    private GetMerchantOrderDetailService getMerchantOrderDetailService;

    @Mock
    private ChangeOrderStatusService changeOrderStatusService;

    @Mock
    private GetMerchantOrderStatusCountsService getMerchantOrderStatusCountsService;

    @Mock
    private CreateMerchantOrderService createMerchantOrderService;

    private MockMvc mockMvc;
    private UUID storeId;
    private UUID merchantUserId;
    private JwtPrincipal currentPrincipal;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        merchantUserId = UUID.randomUUID();
        currentPrincipal = new JwtPrincipal(merchantUserId, Set.of("MERCHANT"));

        MerchantOrderController controller = new MerchantOrderController(
                storeContextService,
                listMerchantOrdersService,
                getMerchantOrderDetailService,
                changeOrderStatusService,
                getMerchantOrderStatusCountsService,
                createMerchantOrderService
        );

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
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
        };

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    private MerchantOrderSummaryResponse createSampleSummary(UUID orderId) {
        return new MerchantOrderSummaryResponse(
                orderId, "JAL-2026-0001", storeId, UUID.randomUUID(),
                OrderStatus.CONFIRMED, DeliveryMode.PICKUP, LocalDate.of(2026, 11, 1),
                LocalTime.of(10, 0), LocalTime.of(12, 0), "Carlos Mendoza",
                "987654321", null, null, new BigDecimal("45.00"), "PEN",
                2, Instant.parse("2026-10-31T10:00:00Z"), Instant.parse("2026-10-31T10:00:00Z"),
                List.of(new MerchantOrderItemResponse(
                        UUID.randomUUID(), UUID.randomUUID(), "Pie de Manzana",
                        "Mediano", 2, new BigDecimal("22.50"), new BigDecimal("45.00")
                ))
        );
    }

    private MerchantOrderDetailResponse createSampleDetail(UUID orderId, OrderStatus orderStatus) {
        return new MerchantOrderDetailResponse(
                orderId, "JAL-2026-0001", storeId, UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), orderStatus, DeliveryMode.PICKUP,
                LocalDate.of(2026, 11, 1), LocalTime.of(10, 0), LocalTime.of(12, 0),
                "Carlos Mendoza", "987654321", "carlos@ejemplo.pe",
                null, null, null, null, "PEN",
                new BigDecimal("45.00"), BigDecimal.ZERO, null,
                BigDecimal.ZERO, new BigDecimal("6.86"), new BigDecimal("45.00"),
                Instant.parse("2026-10-31T10:00:00Z"), Instant.parse("2026-10-31T10:00:00Z"),
                List.of(new MerchantOrderItemResponse(
                        UUID.randomUUID(), UUID.randomUUID(), "Pie de Manzana",
                        "Mediano", 2, new BigDecimal("22.50"), new BigDecimal("45.00")
                )),
                Collections.emptyList()
        );
    }

    @Test
    @DisplayName("1. Comerciante autorizado: GET / listado correcto con metadatos y código 200")
    void shouldListOrdersForAuthorizedMerchant() throws Exception {
        UUID orderId = UUID.randomUUID();
        MerchantOrderSummaryResponse summary = createSampleSummary(orderId);
        MerchantOrderPageResponse pageResponse = new MerchantOrderPageResponse(
                List.of(summary), 0, 20, 1, 1, true, true
        );

        doNothing().when(storeContextService).validateStoreOwnership(storeId, currentPrincipal);
        when(listMerchantOrdersService.execute(eq(storeId), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/merchants/stores/{storeId}/orders", storeId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content[0].id").value(orderId.toString()))
                .andExpect(jsonPath("$.content[0].orderNumber").value("JAL-2026-0001"))
                .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.content[0].customerName").value("Carlos Mendoza"))
                .andExpect(jsonPath("$.content[0].totalAmount").value(45.00))
                .andExpect(jsonPath("$.content[0].items[0].productName").value("Pie de Manzana"));

        verify(storeContextService).validateStoreOwnership(storeId, currentPrincipal);
        verify(listMerchantOrdersService).execute(eq(storeId), isNull(), isNull(), eq(PageRequest.of(0, 20)));
    }

    @Test
    @DisplayName("2. Filtro y paginación: GET / con status, page y size válidos")
    void shouldListOrdersWithStatusFilterAndCustomPagination() throws Exception {
        MerchantOrderPageResponse pageResponse = new MerchantOrderPageResponse(
                Collections.emptyList(), 1, 10, 15, 2, false, true
        );

        doNothing().when(storeContextService).validateStoreOwnership(storeId, currentPrincipal);
        when(listMerchantOrdersService.execute(eq(storeId), eq(OrderStatus.IN_PREPARATION), isNull(), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/merchants/stores/{storeId}/orders", storeId)
                        .param("status", "IN_PREPARATION")
                        .param("page", "1")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(15))
                .andExpect(jsonPath("$.totalPages").value(2));

        verify(listMerchantOrdersService).execute(eq(storeId), eq(OrderStatus.IN_PREPARATION), isNull(), eq(PageRequest.of(1, 10)));
    }

    @Test
    @DisplayName("2b. Búsqueda global con término y status: GET / con search, status y paginación")
    void shouldListOrdersWithSearchTermAndStatus() throws Exception {
        UUID orderId = UUID.randomUUID();
        MerchantOrderSummaryResponse summary = createSampleSummary(orderId);
        MerchantOrderPageResponse pageResponse = new MerchantOrderPageResponse(
                List.of(summary), 0, 10, 1, 1, true, true
        );

        doNothing().when(storeContextService).validateStoreOwnership(storeId, currentPrincipal);
        when(listMerchantOrdersService.execute(eq(storeId), eq(OrderStatus.CONFIRMED), eq("Carlos"), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/merchants/stores/{storeId}/orders", storeId)
                        .param("status", "CONFIRMED")
                        .param("search", "Carlos")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].customerName").value("Carlos Mendoza"));

        verify(listMerchantOrdersService).execute(eq(storeId), eq(OrderStatus.CONFIRMED), eq("Carlos"), eq(PageRequest.of(0, 10)));
    }

    @Test
    @DisplayName("2c. Rechaza término de búsqueda con más de 100 caracteres con HTTP 400")
    void shouldReturnBadRequestWhenSearchExceeds100Chars() throws Exception {
        doNothing().when(storeContextService).validateStoreOwnership(storeId, currentPrincipal);
        String excessiveSearch = "a".repeat(101);

        mockMvc.perform(get("/api/v1/merchants/stores/{storeId}/orders", storeId)
                        .param("search", excessiveSearch)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));

        verifyNoInteractions(listMerchantOrdersService);
    }

    @Test
    @DisplayName("3. Detalle autorizado: GET /{orderId} retorna 200 con MerchantOrderDetailResponse")
    void shouldGetOrderDetailWhenAuthorized() throws Exception {
        UUID orderId = UUID.randomUUID();
        MerchantOrderDetailResponse detail = createSampleDetail(orderId, OrderStatus.CONFIRMED);

        doNothing().when(storeContextService).validateStoreOwnership(storeId, currentPrincipal);
        when(getMerchantOrderDetailService.execute(orderId, storeId)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/merchants/stores/{storeId}/orders/{orderId}", storeId, orderId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.orderNumber").value("JAL-2026-0001"))
                .andExpect(jsonPath("$.storeId").value(storeId.toString()))
                .andExpect(jsonPath("$.customerEmail").value("carlos@ejemplo.pe"))
                .andExpect(jsonPath("$.includedTaxAmount").value(6.86));

        verify(storeContextService).validateStoreOwnership(storeId, currentPrincipal);
        verify(getMerchantOrderDetailService).execute(orderId, storeId);
    }

    @Test
    @DisplayName("4. Pedido inexistente o de otra tienda: GET /{orderId} responde 404 Not Found")
    void shouldReturn404WhenOrderNotFound() throws Exception {
        UUID orderId = UUID.randomUUID();

        doNothing().when(storeContextService).validateStoreOwnership(storeId, currentPrincipal);
        when(getMerchantOrderDetailService.execute(orderId, storeId))
                .thenThrow(new ResourceNotFoundException("Pedido", orderId));

        mockMvc.perform(get("/api/v1/merchants/stores/{storeId}/orders/{orderId}", storeId, orderId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Pedido")));

        verify(getMerchantOrderDetailService).execute(orderId, storeId);
    }

    @Test
    @DisplayName("5. Comerciante intentando acceder a otra tienda: responde 403 Forbidden")
    void shouldReturn403WhenMerchantAttemptsAccessToAnotherStore() throws Exception {
        UUID foreignStoreId = UUID.randomUUID();

        doThrow(new AccessDeniedException("No tiene permisos para operar sobre una tienda ajena."))
                .when(storeContextService).validateStoreOwnership(foreignStoreId, currentPrincipal);

        mockMvc.perform(get("/api/v1/merchants/stores/{storeId}/orders", foreignStoreId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("No tiene permisos para operar sobre una tienda ajena."));

        verifyNoInteractions(listMerchantOrdersService);
    }

    @Test
    @DisplayName("6. Cliente intentando acceder a endpoints de comerciante: responde 403 Forbidden")
    void shouldReturn403WhenCustomerAttemptsAccessToMerchantEndpoints() throws Exception {
        currentPrincipal = new JwtPrincipal(UUID.randomUUID(), Set.of("CUSTOMER"));

        doThrow(new AccessDeniedException("Solo los usuarios con el rol MERCHANT pueden realizar esta operación."))
                .when(storeContextService).validateStoreOwnership(storeId, currentPrincipal);

        mockMvc.perform(get("/api/v1/merchants/stores/{storeId}/orders", storeId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verifyNoInteractions(listMerchantOrdersService);
    }

    @Test
    @DisplayName("7. Ausencia de autenticación: responde 401 Unauthorized")
    void shouldReturn401WhenPrincipalIsNull() throws Exception {
        currentPrincipal = null;

        mockMvc.perform(get("/api/v1/merchants/stores/{storeId}/orders", storeId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Autenticación requerida"));

        verifyNoInteractions(storeContextService);
        verifyNoInteractions(listMerchantOrdersService);
    }

    @Test
    @DisplayName("8. PATCH válido: responde 200 con el detalle actualizado tras aplicar transición")
    void shouldChangeOrderStatusSuccessfully() throws Exception {
        UUID orderId = UUID.randomUUID();
        MerchantOrderDetailResponse updatedDetail = createSampleDetail(orderId, OrderStatus.IN_PREPARATION);

        doNothing().when(storeContextService).validateStoreOwnership(storeId, currentPrincipal);
        doNothing().when(changeOrderStatusService).execute(orderId, OrderStatus.IN_PREPARATION, storeId);
        when(getMerchantOrderDetailService.execute(orderId, storeId)).thenReturn(updatedDetail);

        String body = "{\"status\":\"IN_PREPARATION\"}";

        mockMvc.perform(patch("/api/v1/merchants/stores/{storeId}/orders/{orderId}/status", storeId, orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("IN_PREPARATION"));

        verify(changeOrderStatusService).execute(orderId, OrderStatus.IN_PREPARATION, storeId);
        verify(getMerchantOrderDetailService).execute(orderId, storeId);
    }

    @Test
    @DisplayName("8b. PATCH con motivo: invoca servicio de aplicación pasando motivo de cancelación")
    void shouldChangeOrderStatusWithReasonSuccessfully() throws Exception {
        UUID orderId = UUID.randomUUID();
        MerchantOrderDetailResponse updatedDetail = createSampleDetail(orderId, OrderStatus.CANCELLED);

        doNothing().when(storeContextService).validateStoreOwnership(storeId, currentPrincipal);
        doNothing().when(changeOrderStatusService).execute(orderId, OrderStatus.CANCELLED, storeId, "Insumos agotados");
        when(getMerchantOrderDetailService.execute(orderId, storeId)).thenReturn(updatedDetail);

        String body = "{\"status\":\"CANCELLED\",\"reason\":\"Insumos agotados\"}";

        mockMvc.perform(patch("/api/v1/merchants/stores/{storeId}/orders/{orderId}/status", storeId, orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(changeOrderStatusService).execute(orderId, OrderStatus.CANCELLED, storeId, "Insumos agotados");
        verify(getMerchantOrderDetailService).execute(orderId, storeId);
    }

    @Test
    @DisplayName("9. PATCH idempotente: repetir el estado actual retorna 200 OK con detalle sin error")
    void shouldHandleIdempotentStatusChange() throws Exception {
        UUID orderId = UUID.randomUUID();
        MerchantOrderDetailResponse currentDetail = createSampleDetail(orderId, OrderStatus.CONFIRMED);

        doNothing().when(storeContextService).validateStoreOwnership(storeId, currentPrincipal);
        // El servicio de aplicación realiza retorno temprano no-op
        doNothing().when(changeOrderStatusService).execute(orderId, OrderStatus.CONFIRMED, storeId);
        when(getMerchantOrderDetailService.execute(orderId, storeId)).thenReturn(currentDetail);

        String body = "{\"status\":\"CONFIRMED\"}";

        mockMvc.perform(patch("/api/v1/merchants/stores/{storeId}/orders/{orderId}/status", storeId, orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        verify(changeOrderStatusService).execute(orderId, OrderStatus.CONFIRMED, storeId);
    }

    @Test
    @DisplayName("10. PATCH con transición inválida: responde 422 Unprocessable Content según RN-ORD-07")
    void shouldReturn422WhenTransitionIsInvalid() throws Exception {
        UUID orderId = UUID.randomUUID();

        doNothing().when(storeContextService).validateStoreOwnership(storeId, currentPrincipal);
        doThrow(new BusinessRuleException("RN-ORD-07", "Transición no permitida desde CONFIRMED a READY."))
                .when(changeOrderStatusService).execute(orderId, OrderStatus.READY, storeId);

        String body = "{\"status\":\"READY\"}";

        mockMvc.perform(patch("/api/v1/merchants/stores/{storeId}/orders/{orderId}/status", storeId, orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("RN-ORD-07"))
                .andExpect(jsonPath("$.message").value("Transición no permitida desde CONFIRMED a READY."));

        verify(changeOrderStatusService).execute(orderId, OrderStatus.READY, storeId);
        verifyNoInteractions(getMerchantOrderDetailService);
    }

    @Test
    @DisplayName("11. Parámetros y body inválidos: responde 400 Bad Request")
    void shouldReturn400WhenParametersOrBodyAreInvalid() throws Exception {
        doNothing().when(storeContextService).validateStoreOwnership(eq(storeId), any());

        // A. Page negativo
        mockMvc.perform(get("/api/v1/merchants/stores/{storeId}/orders", storeId)
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());

        // B. Size mayor a 100
        mockMvc.perform(get("/api/v1/merchants/stores/{storeId}/orders", storeId)
                        .param("size", "150"))
                .andExpect(status().isBadRequest());

        // C. Status query param no válido
        mockMvc.perform(get("/api/v1/merchants/stores/{storeId}/orders", storeId)
                        .param("status", "ESTADO_INEXISTENTE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));

        // D. Body status nulo
        UUID orderId = UUID.randomUUID();
        mockMvc.perform(patch("/api/v1/merchants/stores/{storeId}/orders/{orderId}/status", storeId, orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        // E. Body malformado
        mockMvc.perform(patch("/api/v1/merchants/stores/{storeId}/orders/{orderId}/status", storeId, orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ESTADO_INEXISTENTE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    }

    @Test
    @DisplayName("12. Verificación de precedencia: la autorización ocurre antes de invocar cualquier servicio de negocio")
    void shouldVerifyAuthorizationPrecedesBusinessServices() throws Exception {
        UUID alienStoreId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        doThrow(new AccessDeniedException("No tiene permisos para operar sobre una tienda ajena."))
                .when(storeContextService).validateStoreOwnership(alienStoreId, currentPrincipal);

        mockMvc.perform(patch("/api/v1/merchants/stores/{storeId}/orders/{orderId}/status", alienStoreId, orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PREPARATION\"}"))
                .andExpect(status().isForbidden());

        // Garantizar que ningún servicio de aplicación o mutación de negocio fue contactado
        verify(storeContextService).validateStoreOwnership(alienStoreId, currentPrincipal);
        verifyNoInteractions(changeOrderStatusService);
        verifyNoInteractions(getMerchantOrderDetailService);
        verifyNoInteractions(listMerchantOrdersService);
        verifyNoInteractions(getMerchantOrderStatusCountsService);
    }

    @Test
    @DisplayName("13. Consulta de contadores: devuelve histórico completo cuando date no se envía")
    void shouldReturnHistoricalCountsWhenDateNotProvided() throws Exception {
        MerchantOrderStatusCountsResponse expectedResponse = new MerchantOrderStatusCountsResponse(
                48L, 12L, 6L, 4L, 2L, 20L, 4L
        );

        when(getMerchantOrderStatusCountsService.execute(storeId, null)).thenReturn(expectedResponse);

        mockMvc.perform(get("/api/v1/merchants/stores/{storeId}/orders/counts", storeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.all").value(48))
                .andExpect(jsonPath("$.confirmed").value(12))
                .andExpect(jsonPath("$.inPreparation").value(6))
                .andExpect(jsonPath("$.ready").value(4))
                .andExpect(jsonPath("$.outForDelivery").value(2))
                .andExpect(jsonPath("$.completed").value(20))
                .andExpect(jsonPath("$.cancelled").value(4));

        verify(storeContextService).validateStoreOwnership(storeId, currentPrincipal);
        verify(getMerchantOrderStatusCountsService).execute(storeId, null);
    }

    @Test
    @DisplayName("14. Consulta de contadores: devuelve desglose diario cuando date es provisto")
    void shouldReturnDailyCountsWhenDateProvided() throws Exception {
        LocalDate serviceDate = LocalDate.of(2026, 11, 14);
        MerchantOrderStatusCountsResponse expectedResponse = new MerchantOrderStatusCountsResponse(
                10L, 4L, 3L, 2L, 0L, 1L, 0L
        );

        when(getMerchantOrderStatusCountsService.execute(storeId, serviceDate)).thenReturn(expectedResponse);

        mockMvc.perform(get("/api/v1/merchants/stores/{storeId}/orders/counts", storeId)
                        .param("date", "2026-11-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.all").value(10))
                .andExpect(jsonPath("$.confirmed").value(4))
                .andExpect(jsonPath("$.inPreparation").value(3))
                .andExpect(jsonPath("$.ready").value(2))
                .andExpect(jsonPath("$.outForDelivery").value(0))
                .andExpect(jsonPath("$.completed").value(1))
                .andExpect(jsonPath("$.cancelled").value(0));

        verify(storeContextService).validateStoreOwnership(storeId, currentPrincipal);
        verify(getMerchantOrderStatusCountsService).execute(storeId, serviceDate);
    }

    @Test
    @DisplayName("15. Consulta de contadores: retorna 403 Forbidden si la tienda no pertenece al Merchant")
    void shouldReturnForbiddenWhenStoreNotOwnedForCounts() throws Exception {
        UUID alienStoreId = UUID.randomUUID();
        doThrow(new AccessDeniedException("No tiene permisos para operar sobre una tienda ajena."))
                .when(storeContextService).validateStoreOwnership(alienStoreId, currentPrincipal);

        mockMvc.perform(get("/api/v1/merchants/stores/{storeId}/orders/counts", alienStoreId))
                .andExpect(status().isForbidden());

        verify(storeContextService).validateStoreOwnership(alienStoreId, currentPrincipal);
        verifyNoInteractions(getMerchantOrderStatusCountsService);
    }

    @Test
    @DisplayName("16. Consulta de contadores: retorna 401 Unauthorized si principal es nulo")
    void shouldReturnUnauthorizedWhenPrincipalNullForCounts() throws Exception {
        currentPrincipal = null;

        mockMvc.perform(get("/api/v1/merchants/stores/{storeId}/orders/counts", storeId))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(storeContextService);
        verifyNoInteractions(getMerchantOrderStatusCountsService);
    }

    @Test
    @DisplayName("17. Consulta de contadores: retorna 400 Bad Request si date tiene formato inválido")
    void shouldReturnBadRequestWhenDateFormatIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/merchants/stores/{storeId}/orders/counts", storeId)
                        .param("date", "14-11-2026"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(getMerchantOrderStatusCountsService);
    }

    @Test
    @DisplayName("18. Creación de pedido directo: retorna 201 Created cuando el payload es válido")
    void shouldCreateOrderSuccessfully() throws Exception {
        UUID variantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String jsonPayload = """
            {
                "deliveryMode": "PICKUP",
                "customerName": "Juan Pérez",
                "customerPhone": "987654321",
                "customerEmail": "juan@example.com",
                "paymentMethod": "CASH",
                "items": [
                    {
                        "variantId": "%s",
                        "quantity": 2,
                        "unitPriceAmount": 25.50
                    }
                ]
            }
            """.formatted(variantId);

        MerchantOrderDetailResponse mockDetail = new MerchantOrderDetailResponse(
                orderId, "ORD-00000001", storeId, merchantUserId, null, null,
                OrderStatus.CONFIRMED, DeliveryMode.PICKUP, LocalDate.now(), null, null,
                "Juan Pérez", "987654321", "juan@example.com", null, null, null, null,
                "PEN", new BigDecimal("51.00"), BigDecimal.ZERO, null, BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("51.00"), Instant.now(), Instant.now(),
                List.of(), List.of()
        );

        when(createMerchantOrderService.execute(eq(storeId), eq(merchantUserId), any()))
                .thenReturn(mockDetail);

        mockMvc.perform(post("/api/v1/merchants/stores/{storeId}/orders", storeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.orderNumber").value("ORD-00000001"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalAmount").value(51.00));

        verify(storeContextService).validateStoreOwnership(storeId, currentPrincipal);
        verify(createMerchantOrderService).execute(eq(storeId), eq(merchantUserId), any());
    }

    @Test
    @DisplayName("19. Creación de pedido directo: retorna 400 Bad Request si faltan campos obligatorios")
    void shouldReturnBadRequestWhenCreateOrderPayloadIsInvalid() throws Exception {
        String invalidJson = """
            {
                "customerName": "",
                "customerPhone": "invalido",
                "items": []
            }
            """;

        mockMvc.perform(post("/api/v1/merchants/stores/{storeId}/orders", storeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createMerchantOrderService);
    }

    @Test
    @DisplayName("20. Creación de pedido directo: retorna 403 Forbidden si la tienda no pertenece al Merchant")
    void shouldReturnForbiddenWhenStoreNotOwnedForCreateOrder() throws Exception {
        UUID alienStoreId = UUID.randomUUID();
        UUID variantId = UUID.randomUUID();
        String jsonPayload = """
            {
                "deliveryMode": "PICKUP",
                "customerName": "Juan Pérez",
                "customerPhone": "987654321",
                "paymentMethod": "CASH",
                "items": [
                    {
                        "variantId": "%s",
                        "quantity": 1
                    }
                ]
            }
            """.formatted(variantId);

        doThrow(new AccessDeniedException("No tiene permisos para operar sobre una tienda ajena."))
                .when(storeContextService).validateStoreOwnership(alienStoreId, currentPrincipal);

        mockMvc.perform(post("/api/v1/merchants/stores/{storeId}/orders", alienStoreId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isForbidden());

        verify(storeContextService).validateStoreOwnership(alienStoreId, currentPrincipal);
        verifyNoInteractions(createMerchantOrderService);
    }
}

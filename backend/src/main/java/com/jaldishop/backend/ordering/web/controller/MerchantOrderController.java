package com.jaldishop.backend.ordering.web.controller;

import com.jaldishop.backend.ordering.application.service.ChangeOrderStatusService;
import com.jaldishop.backend.ordering.application.service.GetMerchantOrderDetailService;
import com.jaldishop.backend.ordering.application.service.GetMerchantOrderStatusCountsService;
import com.jaldishop.backend.ordering.application.service.ListMerchantOrdersService;
import com.jaldishop.backend.ordering.application.result.MerchantOrderDetailResponse;
import com.jaldishop.backend.ordering.application.result.MerchantOrderPageResponse;
import com.jaldishop.backend.ordering.application.result.MerchantOrderStatusCountsResponse;
import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.ordering.web.dto.ChangeOrderStatusRequest;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import com.jaldishop.backend.store.application.service.StoreContextService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Controlador REST para la gestión y seguimiento de pedidos por parte del comerciante autenticado.
 *
 * <p><strong>Ruta Base:</strong> {@code /api/v1/merchants/stores/{storeId}/orders}</p>
 * <p><strong>Seguridad y Aislamiento:</strong>
 * Protegido con {@code @PreAuthorize("hasRole('MERCHANT')")}.
 * Cada operación valida explícitamente la propiedad de la tienda mediante
 * {@link StoreContextService#validateStoreOwnership(UUID, JwtPrincipal)} antes de invocar la capa de aplicación.</p>
 */
@RestController
@RequestMapping("/api/v1/merchants/stores/{storeId}/orders")
@PreAuthorize("hasRole('MERCHANT')")
@Validated
public class MerchantOrderController {

    private final StoreContextService storeContextService;
    private final ListMerchantOrdersService listMerchantOrdersService;
    private final GetMerchantOrderDetailService getMerchantOrderDetailService;
    private final ChangeOrderStatusService changeOrderStatusService;
    private final GetMerchantOrderStatusCountsService getMerchantOrderStatusCountsService;

    public MerchantOrderController(
            StoreContextService storeContextService,
            ListMerchantOrdersService listMerchantOrdersService,
            GetMerchantOrderDetailService getMerchantOrderDetailService,
            ChangeOrderStatusService changeOrderStatusService,
            GetMerchantOrderStatusCountsService getMerchantOrderStatusCountsService
    ) {
        this.storeContextService = storeContextService;
        this.listMerchantOrdersService = listMerchantOrdersService;
        this.getMerchantOrderDetailService = getMerchantOrderDetailService;
        this.changeOrderStatusService = changeOrderStatusService;
        this.getMerchantOrderStatusCountsService = getMerchantOrderStatusCountsService;
    }

    /**
     * Consulta contadores agregados de pedidos por estado para la tienda.
     * Si no se especifica 'date', devuelve el histórico acumulado completo de la tienda (RN-COUNT-01).
     * Si se especifica 'date', devuelve los contadores de pedidos para esa fecha de servicio (RN-COUNT-04).
     */
    @GetMapping("/counts")
    public ResponseEntity<MerchantOrderStatusCountsResponse> getOrderCounts(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        validatePrincipal(principal);
        storeContextService.validateStoreOwnership(storeId, principal);
        MerchantOrderStatusCountsResponse response = getMerchantOrderStatusCountsService.execute(storeId, date);
        return ResponseEntity.ok(response);
    }

    /**
     * Consulta paginada de pedidos de la tienda, con filtros opcionales por estado y búsqueda textual.
     */
    @GetMapping
    public ResponseEntity<MerchantOrderPageResponse> listOrders(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @Size(max = 100, message = "El término de búsqueda no puede superar los 100 caracteres") String search,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "El número de página no puede ser negativo") int page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "El tamaño de página debe ser al menos 1")
                                               @Max(value = 100, message = "El tamaño de página no puede ser mayor a 100") int size
    ) {
        validatePrincipal(principal);
        storeContextService.validateStoreOwnership(storeId, principal);
        if (search != null && search.length() > 100) {
            throw new IllegalArgumentException("El término de búsqueda no puede superar los 100 caracteres");
        }
        if (page < 0) {
            throw new IllegalArgumentException("El número de página no puede ser negativo");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("El tamaño de página debe estar entre 1 y 100");
        }
        Pageable pageable = PageRequest.of(page, size);
        MerchantOrderPageResponse response = listMerchantOrdersService.execute(storeId, status, search, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Consulta del detalle completo de un pedido perteneciente a la tienda.
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<MerchantOrderDetailResponse> getOrderDetail(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @PathVariable UUID orderId
    ) {
        validatePrincipal(principal);
        storeContextService.validateStoreOwnership(storeId, principal);
        MerchantOrderDetailResponse response = getMerchantOrderDetailService.execute(orderId, storeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Transición de estado de un pedido perteneciente a la tienda.
     * Retorna 200 OK con el detalle actualizado tras aplicar la transición.
     */
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<MerchantOrderDetailResponse> changeOrderStatus(
            @AuthenticationPrincipal JwtPrincipal principal,
            @PathVariable UUID storeId,
            @PathVariable UUID orderId,
            @Valid @RequestBody ChangeOrderStatusRequest request
    ) {
        validatePrincipal(principal);
        storeContextService.validateStoreOwnership(storeId, principal);
        changeOrderStatusService.execute(orderId, request.status(), storeId);
        MerchantOrderDetailResponse updatedOrder = getMerchantOrderDetailService.execute(orderId, storeId);
        return ResponseEntity.ok(updatedOrder);
    }

    private void validatePrincipal(JwtPrincipal principal) {
        if (principal == null) {
            throw new BadCredentialsException("Autenticación requerida");
        }
    }
}

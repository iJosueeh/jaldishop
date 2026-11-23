package com.jaldishop.backend.billing.web.controller;

import com.jaldishop.backend.billing.application.service.MerchantBillingQueryService;
import com.jaldishop.backend.billing.application.service.MerchantPayoutProcessingService;
import com.jaldishop.backend.billing.web.dto.MerchantBillingSummaryResponse;
import com.jaldishop.backend.billing.web.dto.MerchantPayoutDestinationResponse;
import com.jaldishop.backend.billing.web.dto.UpdatePayoutDestinationRequest;
import com.jaldishop.backend.shared.domain.model.Money;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import com.jaldishop.backend.store.application.service.StoreContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantBillingControllerTest {

    @Mock
    private StoreContextService storeContextService;

    @Mock
    private MerchantBillingQueryService billingQueryService;

    @Mock
    private MerchantPayoutProcessingService payoutProcessingService;

    private MerchantBillingController controller;

    private final UUID storeId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final JwtPrincipal principal = new JwtPrincipal(userId, Set.of("ROLE_MERCHANT"));

    @BeforeEach
    void setUp() {
        controller = new MerchantBillingController(storeContextService, billingQueryService, payoutProcessingService);
    }

    @Test
    @DisplayName("Debe retornar resumen de facturación con balance y cabeceras anti-caché")
    void shouldReturnBillingSummaryWithNoCacheHeaders() {
        MerchantBillingSummaryResponse summary = new MerchantBillingSummaryResponse(
                storeId,
                new BigDecimal("120.00"),
                BigDecimal.ZERO,
                new BigDecimal("300.00"),
                new BigDecimal("50.00"),
                "PEN",
                null,
                Instant.now()
        );

        when(billingQueryService.getBillingSummary(storeId)).thenReturn(summary);

        ResponseEntity<MerchantBillingSummaryResponse> response = controller.getBillingSummary(storeId, principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().availableBalance()).isEqualByComparingTo("120.00");
        assertThat(response.getHeaders().getCacheControl()).contains("no-store");
        assertThat(response.getHeaders().getCacheControl()).contains("must-revalidate");
        verify(storeContextService).validateStoreOwnership(storeId, principal);
    }

    @Test
    @DisplayName("Debe solicitar liquidación exitosamente cuando saldo supera umbral y cuenta existe")
    void shouldProcessPayoutWhenBalanceSufficientAndDestinationExists() {
        UUID destId = UUID.randomUUID();
        MerchantPayoutDestinationResponse destination = new MerchantPayoutDestinationResponse(
                destId, storeId, "BANK_TRANSFER_CCI", "BCP", "123456", "00219400123456789012",
                null, "Juan Perez", "DNI", "12345678", true, true, Instant.now()
        );

        MerchantBillingSummaryResponse summary = new MerchantBillingSummaryResponse(
                storeId, new BigDecimal("85.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("50.00"), "PEN", destination, Instant.now()
        );

        UUID batchId = UUID.randomUUID();
        when(billingQueryService.getPrimaryDestination(storeId)).thenReturn(destination);
        when(billingQueryService.getBillingSummary(storeId)).thenReturn(summary);
        when(payoutProcessingService.processPayoutForStore(eq(storeId), eq(destId), any(Money.class))).thenReturn(batchId);

        ResponseEntity<Map<String, Object>> response = controller.requestPayout(storeId, principal);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "PROCESSING");
        assertThat(response.getBody()).containsEntry("batchId", batchId);
    }
}

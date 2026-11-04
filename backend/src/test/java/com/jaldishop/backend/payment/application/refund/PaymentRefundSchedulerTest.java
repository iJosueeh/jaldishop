package com.jaldishop.backend.payment.application.refund;

import com.jaldishop.backend.payment.application.refund.PaymentRefundService.RefundBatchSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentRefundScheduler — Pruebas unitarias del disparador de reembolsos")
class PaymentRefundSchedulerTest {

    @Mock
    private PaymentRefundService refundService;

    @Test
    @DisplayName("Debe invocar el servicio de reembolsos con el batchSize configurado")
    void shouldTriggerRefundServiceWithConfiguredBatchSize() {
        int customBatchSize = 15;
        PaymentRefundScheduler scheduler = new PaymentRefundScheduler(refundService, customBatchSize);

        when(refundService.processPendingRefunds(customBatchSize))
                .thenReturn(new RefundBatchSummary(0, 0, 0, 0, 0));

        scheduler.runRefundBatch();

        verify(refundService, times(1)).processPendingRefunds(customBatchSize);
    }

    @Test
    @DisplayName("Constructor predeterminado debe usar batchSize = 25")
    void defaultConstructorShouldUseBatchSize25() {
        PaymentRefundScheduler scheduler = new PaymentRefundScheduler(refundService);

        when(refundService.processPendingRefunds(25))
                .thenReturn(new RefundBatchSummary(0, 0, 0, 0, 0));

        scheduler.runRefundBatch();

        verify(refundService, times(1)).processPendingRefunds(25);
    }

    @Test
    @DisplayName("Debe capturar excepciones inesperadas sin propagarlas fuera del runner programado")
    void shouldCatchAndLogUnexpectedExceptions() {
        PaymentRefundScheduler scheduler = new PaymentRefundScheduler(refundService, 10);

        when(refundService.processPendingRefunds(10))
                .thenThrow(new RuntimeException("Error inesperado en pasarela"));

        assertDoesNotThrow(() -> scheduler.runRefundBatch());
        verify(refundService, times(1)).processPendingRefunds(10);
    }
}

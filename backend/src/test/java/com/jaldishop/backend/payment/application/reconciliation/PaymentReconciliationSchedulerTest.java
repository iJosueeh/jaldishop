package com.jaldishop.backend.payment.application.reconciliation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentReconciliationScheduler — Pruebas unitarias del disparador")
class PaymentReconciliationSchedulerTest {

    @Mock
    private PaymentReconciliationService reconciliationService;

    @InjectMocks
    private PaymentReconciliationScheduler scheduler;

    @Test
    @DisplayName("Debe invocar el servicio de conciliación en el ciclo programado")
    void shouldTriggerReconciliationService() {
        when(reconciliationService.reconcileOrphanApprovedPayments())
                .thenReturn(new ReconciliationBatchSummary(0, 0, 0, 0, 0, List.of()));

        scheduler.run();

        verify(reconciliationService, times(1)).reconcileOrphanApprovedPayments();
    }

    @Test
    @DisplayName("Debe capturar excepciones inesperadas sin propagarlas fuera del runner programado")
    void shouldCatchAndLogUnexpectedExceptions() {
        when(reconciliationService.reconcileOrphanApprovedPayments())
                .thenThrow(new RuntimeException("Error grave de conexión"));

        assertDoesNotThrow(() -> scheduler.run());
        verify(reconciliationService, times(1)).reconcileOrphanApprovedPayments();
    }
}

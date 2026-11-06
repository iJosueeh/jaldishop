package com.jaldishop.backend.payment.application.investigation;

import com.jaldishop.backend.payment.application.gateway.MercadoPagoGateway;
import com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult;
import com.jaldishop.backend.payment.domain.enums.*;
import com.jaldishop.backend.payment.domain.model.*;
import com.jaldishop.backend.payment.domain.repository.*;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.TransactionDefinition;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ManualOrderInvestigationServiceTest {
    final PaymentAttemptRepository attempts = mock(PaymentAttemptRepository.class);
    final PaymentRepository payments = mock(PaymentRepository.class);
    final MercadoPagoGateway gateway = mock(MercadoPagoGateway.class);
    final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    final UUID paymentId = UUID.randomUUID();
    final PaymentAttempt attempt = PaymentAttempt.create(paymentId, 1, PaymentProvider.MERCADO_PAGO,
            PaymentMethod.values()[0], "key", "ORDERS");
    final Payment payment = mock(Payment.class);
    ManualOrderInvestigationService service;

    @BeforeEach void setup() {
        attempt.restoreRemoteOrderCreationState(RemoteOrderCreationState.UNKNOWN);
        when(attempts.findById(attempt.getId())).thenReturn(Optional.of(attempt));
        when(attempts.findByIdForUpdate(attempt.getId())).thenReturn(Optional.of(attempt));
        when(payments.findByIdForUpdate(paymentId)).thenReturn(Optional.of(payment));
        when(payment.getId()).thenReturn(paymentId);
        when(payment.getAmount()).thenReturn(new BigDecimal("10"));
        when(payment.getCurrency()).thenReturn("PEN");
        service = new ManualOrderInvestigationService(attempts, payments, gateway, new MercadoPagoProperties(),
                jdbc, new ObjectMapper(), new AbstractPlatformTransactionManager() {
            protected Object doGetTransaction() { return new Object(); }
            protected void doBegin(Object t, TransactionDefinition d) {}
            protected void doCommit(DefaultTransactionStatus s) {}
            protected void doRollback(DefaultTransactionStatus s) {}
        });
    }

    void knownId() {
        attempt.recordOrder("ORD1", "https://example.com");
        // Reconstruct the historical UNKNOWN + persisted ID combination.
        attempt.restoreRemoteOrderCreationState(RemoteOrderCreationState.UNKNOWN);
        when(jdbc.queryForList(anyString(), eq(String.class), eq(attempt.getId()), eq("key")))
                .thenReturn(List.of("{\"external_reference\":\"" + paymentId + "\",\"total_amount\":\"10.00\"}"));
    }

    OrderQueryResult remote(String reference) {
        return new OrderQueryResult("ORD1", "processed", null, BigDecimal.TEN, BigDecimal.TEN, reference,
                List.of(new OrderQueryResult.OrderPaymentItem("123", "approved", null, BigDecimal.TEN,
                        "PEN", null, null, null)), null, null);
    }

    @Test void noIdDoesNotSearchOrRetry() {
        assertEquals(ManualOrderInvestigationService.Outcome.INCONCLUSIVE, service.investigate(attempt.getId()).outcome());
        verifyNoInteractions(gateway);
    }

    @Test void locksPaymentBeforeAttemptAfterRemoteGet() {
        knownId();
        when(gateway.queryOrder("ORD1")).thenReturn(remote(paymentId.toString()));
        assertEquals(ManualOrderInvestigationService.Outcome.VERIFIED, service.investigate(attempt.getId()).outcome());
        var order = inOrder(attempts, payments, gateway);
        order.verify(attempts).findById(attempt.getId());
        order.verify(gateway).queryOrder("ORD1");
        order.verify(payments).findByIdForUpdate(paymentId);
        order.verify(attempts).findByIdForUpdate(attempt.getId());
    }

    @Test void auditOnlyPathAlsoLocksPaymentBeforeAttempt() {
        assertEquals(ManualOrderInvestigationService.Outcome.INCONCLUSIVE, service.investigate(attempt.getId()).outcome());
        var order = inOrder(attempts, payments);
        order.verify(attempts).findById(attempt.getId());
        order.verify(payments).findByIdForUpdate(paymentId);
        order.verify(attempts).findByIdForUpdate(attempt.getId());
    }

    @Test void changedOwnerUnderLocksPreventsAssociation() {
        knownId();
        when(gateway.queryOrder("ORD1")).thenReturn(remote(paymentId.toString()));
        PaymentAttempt changed = lockedAttemptWithDifferentOwner();
        when(attempts.findByIdForUpdate(attempt.getId())).thenReturn(Optional.of(changed));
        assertEquals(new ManualOrderInvestigationService.Result(
                ManualOrderInvestigationService.Outcome.CONTRADICTION, "IDENTITY_CHANGED"), service.investigate(attempt.getId()));
        verify(attempts, never()).save(any());
        verify(payments, never()).save(any());
        verify(jdbc, never()).queryForList(anyString(), eq(String.class), any(), any());
    }

    @Test void auditOnlyPathRevalidatesOwnerUnderLocks() {
        PaymentAttempt changed = lockedAttemptWithDifferentOwner();
        when(attempts.findByIdForUpdate(attempt.getId())).thenReturn(Optional.of(changed));
        assertEquals(new ManualOrderInvestigationService.Result(
                ManualOrderInvestigationService.Outcome.CONTRADICTION, "IDENTITY_CHANGED"), service.investigate(attempt.getId()));
        verifyNoInteractions(gateway);
        verify(attempts, never()).save(any());
    }

    private PaymentAttempt lockedAttemptWithDifferentOwner() {
        PaymentAttempt changed = mock(PaymentAttempt.class);
        when(changed.getId()).thenReturn(attempt.getId());
        when(changed.getPaymentId()).thenReturn(UUID.randomUUID());
        when(changed.getRemoteOrderCreationState()).thenReturn(RemoteOrderCreationState.UNKNOWN);
        return changed;
    }
    @Test void historicalReferenceVerifiedRepeatedlyWithoutFinancialWrites() {
        knownId();
        when(gateway.queryOrder("ORD1")).thenReturn(remote(paymentId.toString()));
        assertEquals(ManualOrderInvestigationService.Outcome.VERIFIED, service.investigate(attempt.getId()).outcome());
        assertEquals(ManualOrderInvestigationService.Outcome.VERIFIED, service.investigate(attempt.getId()).outcome());
        verify(attempts, never()).save(any());
        verify(payments, never()).save(any());
        assertEquals(RemoteOrderCreationState.UNKNOWN, attempt.getRemoteOrderCreationState());
    }
    @Test void mismatchedReferenceContradicts() {
        knownId();
        when(gateway.queryOrder("ORD1")).thenReturn(remote("pa1_bad"));
        assertEquals(ManualOrderInvestigationService.Outcome.CONTRADICTION, service.investigate(attempt.getId()).outcome());
    }
    @Test void webhookCreationEvidenceWins() {
        knownId();
        when(gateway.queryOrder("ORD1")).thenAnswer(inv -> {
            attempt.recordOrder("ORD1", "https://example.com");
            return remote(paymentId.toString());
        });
        assertEquals(ManualOrderInvestigationService.Outcome.INCONCLUSIVE, service.investigate(attempt.getId()).outcome());
        assertEquals(RemoteOrderCreationState.CREATED, attempt.getRemoteOrderCreationState());
        verify(attempts, never()).save(any());
    }
    @Test void remoteMissingIsInconclusive() {
        knownId();
        when(gateway.queryOrder("ORD1")).thenThrow(new MercadoPagoException(404, "NOT_FOUND", "private"));
        assertEquals(ManualOrderInvestigationService.Outcome.INCONCLUSIVE, service.investigate(attempt.getId()).outcome());
    }
    @Test void remoteFailureIsTechnical() {
        knownId();
        when(gateway.queryOrder("ORD1")).thenThrow(new MercadoPagoException(503, "SERVER", "private"));
        assertEquals(ManualOrderInvestigationService.Outcome.TECHNICAL_ERROR, service.investigate(attempt.getId()).outcome());
    }

    @Test void simulatedHttpGetVerifiesAndPersistsCreationWithoutFinancialTransition() {
        knownId();
        var builder = org.springframework.web.client.RestClient.builder().baseUrl("https://mp.test");
        var server = org.springframework.test.web.client.MockRestServiceServer.bindTo(builder).build();
        var properties = new MercadoPagoProperties();
        properties.setAccessToken("TEST");
        var adapter = new MercadoPagoRestClientAdapter(builder.build(), properties);
        server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("https://mp.test/v1/orders/ORD1"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.method(org.springframework.http.HttpMethod.GET))
                .andExpect(request -> assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess(
                        "{\"id\":\"ORD1\",\"status\":\"processed\",\"external_reference\":\"" + paymentId
                        + "\",\"total_amount\":\"10.00\",\"checkout_url\":\"https://checkout.test\","
                        + "\"transactions\":{\"payments\":[{\"id\":\"123\",\"currency_id\":\"PEN\"}]}}",
                        org.springframework.http.MediaType.APPLICATION_JSON));
        when(gateway.queryOrder("ORD1")).thenAnswer(inv -> adapter.queryOrder("ORD1"));
        assertEquals(ManualOrderInvestigationService.Outcome.VERIFIED, service.investigate(attempt.getId()).outcome());
        assertEquals(RemoteOrderCreationState.CREATED, attempt.getRemoteOrderCreationState());
        verify(attempts).save(attempt);
        verify(payments, never()).save(any());
        server.verify();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {404, 429, 500})
    void simulatedHttpErrors(int status) {
        knownId();
        var builder = org.springframework.web.client.RestClient.builder().baseUrl("https://mp.test");
        var server = org.springframework.test.web.client.MockRestServiceServer.bindTo(builder).build();
        var properties = new MercadoPagoProperties();
        properties.setAccessToken("TEST");
        var adapter = new MercadoPagoRestClientAdapter(builder.build(), properties);
        server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo("https://mp.test/v1/orders/ORD1"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withStatus(org.springframework.http.HttpStatus.valueOf(status)));
        when(gateway.queryOrder("ORD1")).thenAnswer(inv -> adapter.queryOrder("ORD1"));
        assertEquals(status == 404 ? ManualOrderInvestigationService.Outcome.INCONCLUSIVE : ManualOrderInvestigationService.Outcome.TECHNICAL_ERROR,
                service.investigate(attempt.getId()).outcome());
        verify(attempts, never()).save(any());
        server.verify();
    }

    @Test void duplicateOwnerContradicts() {
        knownId();
        when(gateway.queryOrder("ORD1")).thenReturn(remote(paymentId.toString()));
        var other = PaymentAttempt.create(paymentId, 2, PaymentProvider.MERCADO_PAGO, PaymentMethod.values()[0], "other", "ORDERS");
        when(attempts.findByProviderOrderId("ORD1")).thenReturn(Optional.of(other));
        assertEquals(ManualOrderInvestigationService.Outcome.CONTRADICTION, service.investigate(attempt.getId()).outcome());
    }

    @Test void missingCurrencyIsInconclusive() {
        knownId();
        when(gateway.queryOrder("ORD1")).thenReturn(new OrderQueryResult("ORD1", "created", null, BigDecimal.TEN,
                null, paymentId.toString(), List.of(), null, null));
        assertEquals(ManualOrderInvestigationService.Outcome.INCONCLUSIVE, service.investigate(attempt.getId()).outcome());
        verify(attempts, never()).save(any());
    }

    @Test void concurrentFinancialApprovalIsPreserved() {
        knownId();
        when(gateway.queryOrder("ORD1")).thenAnswer(inv -> {
            attempt.markApproved("123", "approved", "accredited", null, null);
            return remote(paymentId.toString());
        });
        assertEquals(ManualOrderInvestigationService.Outcome.VERIFIED, service.investigate(attempt.getId()).outcome());
        assertEquals(PaymentAttemptStatus.APPROVED, attempt.getStatus());
        verify(payments, never()).save(any());
    }
    @Test void missingSnapshotIsInconclusive() {
        knownId();
        when(jdbc.queryForList(anyString(), eq(String.class), eq(attempt.getId()), eq("key"))).thenReturn(List.of());
        when(gateway.queryOrder("ORD1")).thenReturn(remote(paymentId.toString()));
        assertEquals(ManualOrderInvestigationService.Outcome.INCONCLUSIVE, service.investigate(attempt.getId()).outcome());
    }
    @Test void amountMismatchContradicts() {
        knownId();
        when(payment.getAmount()).thenReturn(BigDecimal.ONE);
        when(gateway.queryOrder("ORD1")).thenReturn(remote(paymentId.toString()));
        assertEquals(ManualOrderInvestigationService.Outcome.CONTRADICTION, service.investigate(attempt.getId()).outcome());
    }
}

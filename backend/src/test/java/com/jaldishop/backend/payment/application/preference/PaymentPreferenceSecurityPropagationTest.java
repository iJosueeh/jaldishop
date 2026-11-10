package com.jaldishop.backend.payment.application.preference;

import com.jaldishop.backend.payment.application.gateway.MercadoPagoGateway;
import com.jaldishop.backend.payment.application.gateway.dto.CreateOrderCommand;
import com.jaldishop.backend.payment.domain.enums.PaymentMethod;
import com.jaldishop.backend.payment.domain.enums.PaymentProvider;
import com.jaldishop.backend.payment.domain.enums.RemoteOrderCreationState;
import com.jaldishop.backend.payment.domain.model.PaymentAttempt;
import com.jaldishop.backend.payment.domain.repository.PaymentAttemptRepository;
import com.jaldishop.backend.payment.domain.repository.PaymentRepository;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoOrderSecurityException;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentPreferenceSecurityPropagationTest {

    @Test
    @DisplayName("recordOrderCreationSecurityError tiene configurado formalmente REQUIRES_NEW")
    void shouldHaveRequiresNewAnnotation() throws NoSuchMethodException {
        Method method = PaymentPreferencePreparationService.class.getMethod(
                "recordOrderCreationSecurityError", UUID.class, UUID.class, String.class, String.class
        );

        Transactional annotation = method.getAnnotation(Transactional.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }

    @Test
    @DisplayName("Descarte de self-invocation: CreatePaymentPreferenceService delega en bean externo PaymentPreferencePreparationService")
    void shouldNotBeSelfInvocation() {
        // CreatePaymentPreferenceService no declara recordOrderCreationSecurityError
        Method[] methods = CreatePaymentPreferenceService.class.getDeclaredMethods();
        for (Method m : methods) {
            assertThat(m.getName()).isNotEqualTo("recordOrderCreationSecurityError");
        }

        // Se invoca a través de la referencia inyectada preparationService
        assertThat(PaymentPreferencePreparationService.class)
                .isNotEqualTo(CreatePaymentPreferenceService.class);
    }

    @Configuration
    @EnableTransactionManagement
    static class TxConfig {
        @Bean
        public PlatformTransactionManager transactionManager() {
            PlatformTransactionManager tm = mock(PlatformTransactionManager.class);
            TransactionStatus status = mock(TransactionStatus.class);
            when(tm.getTransaction(any())).thenReturn(status);
            return tm;
        }

        @Bean
        public PaymentRepository paymentRepository() {
            return mock(PaymentRepository.class);
        }

        @Bean
        public PaymentAttemptRepository paymentAttemptRepository() {
            return mock(PaymentAttemptRepository.class);
        }

        @Bean
        public PaymentPreferencePreparationService preparationService(
                PaymentRepository paymentRepository,
                PaymentAttemptRepository paymentAttemptRepository
        ) {
            return new PaymentPreferencePreparationService(
                    null, null, null, null, null,
                    paymentRepository, paymentAttemptRepository,
                    new tools.jackson.databind.ObjectMapper(),
                    null
            );
        }
    }

    @Test
    @DisplayName("Spring AOP intercepta recordOrderCreationSecurityError solicitando PROPAGATION_REQUIRES_NEW y commitea de inmediato")
    void shouldTriggerRequiresNewThroughSpringProxy() {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(TxConfig.class)) {
            PaymentPreferencePreparationService proxyService = ctx.getBean(PaymentPreferencePreparationService.class);
            PlatformTransactionManager txManager = ctx.getBean(PlatformTransactionManager.class);
            PaymentRepository paymentRepository = ctx.getBean(PaymentRepository.class);
            PaymentAttemptRepository attemptRepository = ctx.getBean(PaymentAttemptRepository.class);

            // Verificar que Spring generó un proxy AOP para la clase
            assertThat(AopUtils.isAopProxy(proxyService)).isTrue();

            UUID paymentId = UUID.randomUUID();
            UUID attemptId = UUID.randomUUID();

            com.jaldishop.backend.payment.domain.model.Payment mockPayment = mock(com.jaldishop.backend.payment.domain.model.Payment.class);
            when(paymentRepository.findByIdForUpdate(paymentId)).thenReturn(Optional.of(mockPayment));

            PaymentAttempt attempt = PaymentAttempt.create(
                    paymentId, 1, PaymentProvider.MERCADO_PAGO, PaymentMethod.CARD, "idemp-key-tx", "ORDERS"
            );
            when(attemptRepository.findByIdForUpdate(attemptId)).thenReturn(Optional.of(attempt));

            // Invocar el método en el proxy de Spring
            proxyService.recordOrderCreationSecurityError(paymentId, attemptId, "ORD-REMOTE-TX", "SECURITY_ERROR_LIVE_MODE_MISMATCH");

            // Verificar que Spring TransactionManager solicitó una transacción REQUIRES_NEW
            ArgumentCaptor<TransactionDefinition> defCaptor = ArgumentCaptor.forClass(TransactionDefinition.class);
            verify(txManager).getTransaction(defCaptor.capture());
            assertThat(defCaptor.getValue().getPropagationBehavior())
                    .isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

            // Verificar que la transacción se confirmó (commit) inmediatamente al salir del método
            verify(txManager).commit(any(TransactionStatus.class));

            // Verificar que el estado del attempt fue actualizado a REJECTED_CONFIRMED con código de seguridad
            assertThat(attempt.getStatus()).isEqualTo(com.jaldishop.backend.payment.domain.enums.PaymentAttemptStatus.ERROR);
            assertThat(attempt.getErrorCode()).isEqualTo("SECURITY_ERROR_LIVE_MODE_MISMATCH");
            assertThat(attempt.getErrorMessage()).contains("ORD-REMOTE-TX");
            assertThat(attempt.getRemoteOrderCreationState()).isEqualTo(RemoteOrderCreationState.REJECTED_CONFIRMED);
        }
    }
}

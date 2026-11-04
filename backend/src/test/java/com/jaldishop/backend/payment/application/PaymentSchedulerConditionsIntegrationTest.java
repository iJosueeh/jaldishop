package com.jaldishop.backend.payment.application;

import com.jaldishop.backend.payment.application.reconciliation.PaymentReconciliationScheduler;
import com.jaldishop.backend.payment.application.reconciliation.PaymentReconciliationService;
import com.jaldishop.backend.payment.application.refund.PaymentRefundScheduler;
import com.jaldishop.backend.payment.application.refund.PaymentRefundService;
import com.jaldishop.backend.shared.config.SchedulingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentSchedulerConditionsIntegrationTest — Validación de condiciones de activación de schedulers")
class PaymentSchedulerConditionsIntegrationTest {

    @Configuration
    static class MockServicesConfig {
        @Bean
        PaymentReconciliationService reconciliationService() {
            return Mockito.mock(PaymentReconciliationService.class);
        }

        @Bean
        PaymentRefundService refundService() {
            return Mockito.mock(PaymentRefundService.class);
        }
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SchedulingConfig.class))
            .withUserConfiguration(
                    MockServicesConfig.class,
                    PaymentReconciliationScheduler.class,
                    PaymentRefundScheduler.class
            );

    @Test
    @DisplayName("1. Desactivado por defecto: ningún scheduler debe instanciarse")
    void whenPropertiesDisabled_noSchedulersInstantiated() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(SchedulingConfig.class);
            assertThat(context).hasSingleBean(ScheduledAnnotationBeanPostProcessor.class);
            assertThat(context).doesNotHaveBean(PaymentReconciliationScheduler.class);
            assertThat(context).doesNotHaveBean(PaymentRefundScheduler.class);
        });
    }

    @Test
    @DisplayName("2. Solo conciliación activada: únicamente PaymentReconciliationScheduler debe instanciarse")
    void whenOnlyReconciliationEnabled_onlyReconciliationSchedulerInstantiated() {
        contextRunner
                .withPropertyValues("app.payment.reconciliation.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(PaymentReconciliationScheduler.class);
                    assertThat(context).doesNotHaveBean(PaymentRefundScheduler.class);
                });
    }

    @Test
    @DisplayName("3. Solo reembolsos activados: únicamente PaymentRefundScheduler debe instanciarse")
    void whenOnlyRefundEnabled_onlyRefundSchedulerInstantiated() {
        contextRunner
                .withPropertyValues("payments.mercadopago.refund-scheduler-enabled=true")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(PaymentReconciliationScheduler.class);
                    assertThat(context).hasSingleBean(PaymentRefundScheduler.class);
                });
    }

    @Test
    @DisplayName("4. Ambos activados: ambos schedulers deben instanciarse")
    void whenBothEnabled_bothSchedulersInstantiated() {
        contextRunner
                .withPropertyValues(
                        "app.payment.reconciliation.enabled=true",
                        "payments.mercadopago.refund-scheduler-enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(PaymentReconciliationScheduler.class);
                    assertThat(context).hasSingleBean(PaymentRefundScheduler.class);
                });
    }
}

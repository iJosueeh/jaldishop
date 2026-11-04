package com.jaldishop.backend.realtime.infrastructure.listener;

import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.ordering.domain.event.OrderConfirmedDomainEvent;
import com.jaldishop.backend.ordering.domain.event.OrderStatusChangedDomainEvent;
import com.jaldishop.backend.realtime.application.service.RealtimeNotificationPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = OrderDomainEventListenerTransactionalTest.TestConfig.class)
class OrderDomainEventListenerTransactionalTest {

    @Autowired
    private TestOrderService testOrderService;

    @Autowired
    private RealtimeNotificationPublisher mockPublisher;

    @Test
    @DisplayName("AFTER_COMMIT: La publicación ocurre únicamente cuando la transacción hace COMMIT exitoso")
    void eventIsPublishedWhenTransactionCommits() {
        OrderConfirmedDomainEvent event = new OrderConfirmedDomainEvent(
                UUID.randomUUID(),
                "JAL-2026-000001",
                UUID.randomUUID(),
                BigDecimal.valueOf(50.0),
                "PEN"
        );

        testOrderService.executeWithCommit(event);

        verify(mockPublisher, times(1)).publishOrderConfirmed(event);
    }

    @Test
    @DisplayName("AFTER_COMMIT: No se emite publicación si la transacción hace ROLLBACK")
    void eventIsNotPublishedWhenTransactionRollsBack() {
        OrderConfirmedDomainEvent event = new OrderConfirmedDomainEvent(
                UUID.randomUUID(),
                "JAL-2026-000002",
                UUID.randomUUID(),
                BigDecimal.valueOf(75.0),
                "PEN"
        );

        assertThrows(RuntimeException.class, () ->
                testOrderService.executeWithRollback(event)
        );

        verify(mockPublisher, never()).publishOrderConfirmed(event);
    }

    @Test
    @DisplayName("AFTER_COMMIT: No se emite cambio de estado si la transacción de actualización hace ROLLBACK")
    void statusChangeEventIsNotPublishedWhenTransactionRollsBack() {
        OrderStatusChangedDomainEvent event = new OrderStatusChangedDomainEvent(
                UUID.randomUUID(),
                "JAL-2026-000003",
                UUID.randomUUID(),
                UUID.randomUUID(),
                OrderStatus.CONFIRMED,
                OrderStatus.IN_PREPARATION
        );

        assertThrows(RuntimeException.class, () ->
                testOrderService.executeStatusChangeWithRollback(event)
        );

        verify(mockPublisher, never()).publishOrderStatusChanged(event);
    }

    @Configuration
    @EnableTransactionManagement
    static class TestConfig {

        @Bean
        public PlatformTransactionManager transactionManager() {
            return new AbstractPlatformTransactionManager() {
                @Override
                protected Object doGetTransaction() {
                    return new Object();
                }

                @Override
                protected void doBegin(Object transaction, TransactionDefinition definition) {}

                @Override
                protected void doCommit(DefaultTransactionStatus status) {}

                @Override
                protected void doRollback(DefaultTransactionStatus status) {}
            };
        }

        @Bean
        public RealtimeNotificationPublisher realtimeNotificationPublisher() {
            return mock(RealtimeNotificationPublisher.class);
        }

        @Bean
        public OrderDomainEventListener orderDomainEventListener(RealtimeNotificationPublisher publisher) {
            return new OrderDomainEventListener(publisher);
        }

        @Bean
        public TestOrderService testOrderService(ApplicationEventPublisher publisher) {
            return new TestOrderService(publisher);
        }

    }

    static class TestOrderService {

        private final ApplicationEventPublisher eventPublisher;

        public TestOrderService(ApplicationEventPublisher eventPublisher) {
            this.eventPublisher = eventPublisher;
        }

        @Transactional
        public void executeWithCommit(OrderConfirmedDomainEvent event) {
            eventPublisher.publishEvent(event);
        }

        @Transactional
        public void executeWithRollback(OrderConfirmedDomainEvent event) {
            eventPublisher.publishEvent(event);
            throw new RuntimeException("Simulated business failure requiring rollback");
        }

        @Transactional
        public void executeStatusChangeWithRollback(OrderStatusChangedDomainEvent event) {
            eventPublisher.publishEvent(event);
            throw new RuntimeException("Simulated status change failure requiring rollback");
        }

    }

}

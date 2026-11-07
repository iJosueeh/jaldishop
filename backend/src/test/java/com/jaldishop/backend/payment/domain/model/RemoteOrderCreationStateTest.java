package com.jaldishop.backend.payment.domain.model;

import com.jaldishop.backend.payment.domain.enums.*;
import com.jaldishop.backend.payment.infrastructure.persistence.mapper.PaymentAttemptPersistenceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class RemoteOrderCreationStateTest {
    private PaymentAttempt orders() {
        return PaymentAttempt.create(UUID.randomUUID(), 1, PaymentProvider.MERCADO_PAGO,
                PaymentMethod.CARD, UUID.randomUUID().toString(), "ORDERS");
    }

    @Test void creationDoesNotApprovePayment() {
        var attempt = orders();
        assertThat(attempt.getRemoteOrderCreationState()).isEqualTo(RemoteOrderCreationState.PREPARED);
        attempt.recordOrderCreationFailure(false);
        assertThat(attempt.getRemoteOrderCreationState()).isEqualTo(RemoteOrderCreationState.UNKNOWN);
        attempt.recordOrder("ORD-1", "https://example.invalid/checkout");
        assertThat(attempt.getRemoteOrderCreationState()).isEqualTo(RemoteOrderCreationState.CREATED);
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.CREATED);
        attempt.recordOrderCreationFailure(false);
        assertThat(attempt.getRemoteOrderCreationState()).isEqualTo(RemoteOrderCreationState.CREATED);
        assertThatThrownBy(() -> attempt.recordOrder("ORD-2", "https://example.invalid"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test void confirmedRejectionIsIndependentAndCannotBecomeCreation() {
        var attempt = orders();
        attempt.recordOrderCreationFailure(true);
        assertThat(attempt.getRemoteOrderCreationState()).isEqualTo(RemoteOrderCreationState.REJECTED_CONFIRMED);
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.CREATED);
        attempt.recordOrderCreationFailure(false);
        assertThat(attempt.getRemoteOrderCreationState()).isEqualTo(RemoteOrderCreationState.REJECTED_CONFIRMED);
        assertThatThrownBy(() -> attempt.recordOrder("ORD-1", "https://example.invalid"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test void incompleteResponseCannotVerifyCreation() {
        var attempt = orders();
        assertThatThrownBy(() -> attempt.recordOrder("ORD-1", null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(attempt.getProviderOrderId()).isNull();
        assertThat(attempt.getRemoteOrderCreationState()).isEqualTo(RemoteOrderCreationState.PREPARED);
    }

    @ParameterizedTest @EnumSource(RemoteOrderCreationState.class)
    void persistenceMappingPreservesEveryState(RemoteOrderCreationState state) {
        var attempt = orders();
        attempt.restoreRemoteOrderCreationState(state);
        var mapper = new PaymentAttemptPersistenceMapper();
        var loaded = mapper.toDomain(mapper.toEntity(attempt));
        assertThat(loaded.getRemoteOrderCreationState()).isEqualTo(state);
        assertThat(loaded.getStatus()).isEqualTo(attempt.getStatus());
    }

    @Test void legacyNullRemainsConservativeAndPreferencesUnaffected() {
        var mapper = new PaymentAttemptPersistenceMapper();
        var entity = mapper.toEntity(orders());
        entity.setRemoteOrderCreationState(null);
        assertThat(mapper.toDomain(entity).getRemoteOrderCreationState()).isEqualTo(RemoteOrderCreationState.UNKNOWN);
        var preference = PaymentAttempt.create(UUID.randomUUID(), 1, PaymentProvider.MERCADO_PAGO, PaymentMethod.CARD, "key");
        assertThat(mapper.toDomain(mapper.toEntity(preference)).getRemoteOrderCreationState()).isNull();
    }
}

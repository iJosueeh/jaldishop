package com.jaldishop.backend.payment.infrastructure.persistence.adapter;

import com.jaldishop.backend.payment.domain.enums.PaymentAttemptStatus;
import com.jaldishop.backend.payment.domain.enums.PaymentMethod;
import com.jaldishop.backend.payment.domain.enums.PaymentProvider;
import com.jaldishop.backend.payment.domain.model.PaymentAttempt;
import com.jaldishop.backend.payment.infrastructure.persistence.entity.PaymentAttemptEntity;
import com.jaldishop.backend.payment.infrastructure.persistence.mapper.PaymentAttemptPersistenceMapper;
import com.jaldishop.backend.payment.infrastructure.persistence.repository.PaymentAttemptJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JpaPaymentAttemptRepositoryAdapterTest {

    @Mock
    private PaymentAttemptJpaRepository jpaRepository;

    private PaymentAttemptPersistenceMapper mapper;
    private JpaPaymentAttemptRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        mapper = new PaymentAttemptPersistenceMapper();
        adapter = new JpaPaymentAttemptRepositoryAdapter(jpaRepository, mapper);
    }

    @Test
    @DisplayName("findByProviderOrderId busca en JPA repository y mapea a dominio correctamente")
    void shouldFindByProviderOrderId() {
        UUID id = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        String providerOrderId = "ORD01J67CQQH5904WDBVZEM4JMEP3";
        Instant now = Instant.now();

        PaymentAttemptEntity entity = new PaymentAttemptEntity(
                id, paymentId, 1, "MERCADO_PAGO", "CARD", "idemp-001",
                null, providerOrderId, "https://mp.com/checkout", "PAY-111", "approved", "accredited",
                "APPROVED", null, null, now, now, now
        );

        when(jpaRepository.findByProviderOrderId(providerOrderId)).thenReturn(Optional.of(entity));

        Optional<PaymentAttempt> result = adapter.findByProviderOrderId(providerOrderId);

        assertThat(result).isPresent();
        PaymentAttempt attempt = result.get();
        assertThat(attempt.getId()).isEqualTo(id);
        assertThat(attempt.getProviderOrderId()).isEqualTo(providerOrderId);
        assertThat(attempt.getPreferenceId()).isNull();
        assertThat(attempt.getProviderPaymentId()).isEqualTo("PAY-111");

        verify(jpaRepository).findByProviderOrderId(providerOrderId);
    }

    @Test
    @DisplayName("findByPreferenceId conserva soporte para búsquedas históricas con Preferences API")
    void shouldFindByPreferenceId() {
        UUID id = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        String preferenceId = "pref-123456";
        Instant now = Instant.now();

        PaymentAttemptEntity entity = new PaymentAttemptEntity(
                id, paymentId, 1, "MERCADO_PAGO", "WALLET_PURCHASE", "idemp-002",
                preferenceId, null, "https://mp.com/pref", "PAY-222", "approved", "accredited",
                "APPROVED", null, null, now, now, now
        );

        when(jpaRepository.findByPreferenceId(preferenceId)).thenReturn(Optional.of(entity));

        Optional<PaymentAttempt> result = adapter.findByPreferenceId(preferenceId);

        assertThat(result).isPresent();
        PaymentAttempt attempt = result.get();
        assertThat(attempt.getId()).isEqualTo(id);
        assertThat(attempt.getPreferenceId()).isEqualTo(preferenceId);
        assertThat(attempt.getProviderOrderId()).isNull();
        assertThat(attempt.getProviderPaymentId()).isEqualTo("PAY-222");

        verify(jpaRepository).findByPreferenceId(preferenceId);
    }

    @Test
    @DisplayName("save mapea de dominio a entidad y persiste en JPA repository")
    void shouldSavePaymentAttempt() {
        PaymentAttempt domain = PaymentAttempt.create(
                UUID.randomUUID(), 1, PaymentProvider.MERCADO_PAGO, PaymentMethod.CARD, "idemp-save-1"
        );
        domain.recordOrder("ORD-SAVE-1", "https://mp.com/order-save");

        when(jpaRepository.save(any(PaymentAttemptEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentAttempt saved = adapter.save(domain);

        assertThat(saved).isNotNull();
        assertThat(saved.getProviderOrderId()).isEqualTo("ORD-SAVE-1");
        assertThat(saved.getIdempotencyKey()).isEqualTo("idemp-save-1");
        verify(jpaRepository).save(any(PaymentAttemptEntity.class));
    }

}

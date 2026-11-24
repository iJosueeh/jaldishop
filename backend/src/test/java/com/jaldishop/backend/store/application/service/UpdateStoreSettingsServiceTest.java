package com.jaldishop.backend.store.application.service;

import com.jaldishop.backend.identity.application.service.AdminAuditService;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.model.StoreSettings;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
import com.jaldishop.backend.store.domain.repository.StoreSettingsRepository;
import com.jaldishop.backend.store.web.dto.StoreSettingsRequest;
import com.jaldishop.backend.store.web.dto.StoreSettingsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateStoreSettingsServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreSettingsRepository storeSettingsRepository;

    @Mock
    private AdminAuditService auditService;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("Debe rechazar actualización si existe configuración y falta version")
    void shouldRejectMissingVersion() {
        UUID userId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Store store = mock(Store.class);
        when(store.getId()).thenReturn(storeId);
        when(storeRepository.findByOwnerUserId(userId)).thenReturn(Optional.of(store));
        when(storeSettingsRepository.findByStoreId(storeId)).thenReturn(Optional.of(StoreSettings.defaultSettings(storeId)));

        UpdateStoreSettingsService service = new UpdateStoreSettingsService(
                storeRepository, storeSettingsRepository, auditService, userRepository, null
        );

        StoreSettingsRequest request = new StoreSettingsRequest(
                "Cancelación válida", "Reembolso válido",
                false, true, true, true, false,
                BigDecimal.TEN, 30, true, 80, true, null
        );

        assertThatThrownBy(() -> service.execute(userId, request, "10.0.0.2"))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    @DisplayName("Debe sanitizar HTML en políticas y registrar auditoría")
    void shouldSanitizePoliciesAndAudit() {
        UUID userId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Store store = mock(Store.class);
        when(store.getId()).thenReturn(storeId);
        when(storeRepository.findByOwnerUserId(userId)).thenReturn(Optional.of(store));
        when(storeSettingsRepository.findByStoreId(storeId)).thenReturn(Optional.of(StoreSettings.defaultSettings(storeId)));
        when(storeSettingsRepository.save(any(StoreSettings.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        UpdateStoreSettingsService service = new UpdateStoreSettingsService(
                storeRepository, storeSettingsRepository, auditService, userRepository, null
        );

        StoreSettingsRequest request = new StoreSettingsRequest(
                "<p>Cancelación <b>gratuita</b></p>",
                "<script>x</script>Reembolso 24h",
                true, false, true, false, false,
                new BigDecimal("25.00"), 20, true, 75, true, 0L
        );

        StoreSettingsResponse response = service.execute(userId, request, "10.0.0.2");

        assertThat(response.cancellationPolicy()).isEqualTo("Cancelación gratuita");
        assertThat(response.refundPolicy()).isEqualTo("xReembolso 24h");
        verify(auditService).logSuccess(
                eq(userId),
                eq(null),
                eq("UPDATE_STORE_SETTINGS"),
                eq("STORE_SETTINGS"),
                eq(storeId),
                eq("10.0.0.2"),
                any()
        );
    }

    @Test
    @DisplayName("Debe rechazar política vacía tras sanitizar HTML")
    void shouldRejectBlankPolicyAfterSanitize() {
        UUID userId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        Store store = mock(Store.class);
        when(store.getId()).thenReturn(storeId);
        when(storeRepository.findByOwnerUserId(userId)).thenReturn(Optional.of(store));
        when(storeSettingsRepository.findByStoreId(storeId)).thenReturn(Optional.of(StoreSettings.defaultSettings(storeId)));

        UpdateStoreSettingsService service = new UpdateStoreSettingsService(
                storeRepository, storeSettingsRepository, auditService, userRepository, null
        );

        StoreSettingsRequest request = new StoreSettingsRequest(
                "<div>   </div>", "Reembolso válido",
                false, true, true, true, false,
                null, 30, true, 80, true, 0L
        );

        assertThatThrownBy(() -> service.execute(userId, request, "10.0.0.2"))
                .isInstanceOf(BusinessRuleException.class);
    }
}

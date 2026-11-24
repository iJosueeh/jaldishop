package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.application.command.UpdatePlatformSettingsCommand;
import com.jaldishop.backend.identity.application.port.PlatformSettingsProvider;
import com.jaldishop.backend.identity.domain.model.PlatformSettings;
import com.jaldishop.backend.identity.domain.repository.PlatformSettingsRepository;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformSettingsServiceTest {

    @Mock
    private PlatformSettingsRepository repository;

    @Mock
    private PlatformSettingsProvider provider;

    @Mock
    private AdminAuditService auditService;

    @InjectMocks
    private PlatformSettingsService service;

    @Test
    @DisplayName("Debe rechazar actualización si ya existe configuración y falta version (optimistic locking estricto)")
    void shouldRejectMissingVersionWhenPersisted() {
        PlatformSettings existing = PlatformSettings.defaultSettings();
        when(repository.findSingleton()).thenReturn(Optional.of(existing));

        UpdatePlatformSettingsCommand command = new UpdatePlatformSettingsCommand(
                true, false, false, "Aviso", new BigDecimal("5.00"), null
        );

        assertThatThrownBy(() -> service.updateSettings(command, UUID.randomUUID(), "admin@test.com", "127.0.0.1"))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    @DisplayName("Debe rechazar actualización si version no coincide")
    void shouldRejectMismatchedVersion() {
        PlatformSettings existing = new PlatformSettings(
                1, true, false, false, "Aviso", new BigDecimal("5.00"), 3L, Instant.now(), Instant.now()
        );
        when(repository.findSingleton()).thenReturn(Optional.of(existing));

        UpdatePlatformSettingsCommand command = new UpdatePlatformSettingsCommand(
                true, false, false, "Aviso", new BigDecimal("5.00"), 2L
        );

        assertThatThrownBy(() -> service.updateSettings(command, UUID.randomUUID(), "admin@test.com", "127.0.0.1"))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    @DisplayName("Debe sanitizar etiquetas HTML en maintenanceNotice y actualizar caché + auditoría")
    void shouldSanitizeHtmlAndAudit() {
        PlatformSettings existing = PlatformSettings.defaultSettings();
        when(repository.findSingleton()).thenReturn(Optional.of(existing));
        when(repository.save(any(PlatformSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdatePlatformSettingsCommand command = new UpdatePlatformSettingsCommand(
                false, true, true, "<b>Aviso</b> urgente", new BigDecimal("6.50"), 0L
        );

        UUID actorId = UUID.randomUUID();
        PlatformSettings result = service.updateSettings(command, actorId, "admin@test.com", "10.0.0.1");

        assertThat(result.maintenanceNotice()).isEqualTo("Aviso urgente");
        assertThat(result.marketplaceCommissionRate()).isEqualByComparingTo("6.50");

        ArgumentCaptor<PlatformSettings> cacheCaptor = ArgumentCaptor.forClass(PlatformSettings.class);
        verify(provider).updateSettingsCache(cacheCaptor.capture());
        assertThat(cacheCaptor.getValue().maintenanceNotice()).isEqualTo("Aviso urgente");

        verify(auditService).logSuccess(
                eq(actorId),
                eq("admin@test.com"),
                eq("UPDATE_PLATFORM_SETTINGS"),
                eq("PLATFORM_SETTINGS"),
                eq(null),
                eq("10.0.0.1"),
                any()
        );
    }

    @Test
    @DisplayName("Debe rechazar modo mantenimiento activo con aviso vacío tras sanitizar")
    void shouldRejectBlankNoticeWhenMaintenanceActive() {
        PlatformSettings existing = PlatformSettings.defaultSettings();
        when(repository.findSingleton()).thenReturn(Optional.of(existing));

        UpdatePlatformSettingsCommand command = new UpdatePlatformSettingsCommand(
                true, false, true, "<script></script>   ", new BigDecimal("5.00"), 0L
        );

        assertThatThrownBy(() -> service.updateSettings(command, UUID.randomUUID(), "admin@test.com", "127.0.0.1"))
                .isInstanceOf(BusinessRuleException.class);
    }
}

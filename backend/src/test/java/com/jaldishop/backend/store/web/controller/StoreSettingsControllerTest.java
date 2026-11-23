package com.jaldishop.backend.store.web.controller;

import com.jaldishop.backend.shared.security.JwtPrincipal;
import com.jaldishop.backend.store.application.port.in.GetStoreSettingsUseCase;
import com.jaldishop.backend.store.application.port.in.UpdateStoreSettingsUseCase;
import com.jaldishop.backend.store.web.dto.StoreSettingsRequest;
import com.jaldishop.backend.store.web.dto.StoreSettingsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreSettingsControllerTest {

    @Mock
    private GetStoreSettingsUseCase getStoreSettingsUseCase;

    @Mock
    private UpdateStoreSettingsUseCase updateStoreSettingsUseCase;

    @InjectMocks
    private StoreSettingsController controller;

    private final UUID userId = UUID.randomUUID();
    private final JwtPrincipal principal = new JwtPrincipal(userId, java.util.Set.of("ROLE_MERCHANT"));

    @Test
    @DisplayName("Debe retornar los settings de la tienda del usuario autenticado")
    void shouldReturnStoreSettings() {
        StoreSettingsResponse expected = new StoreSettingsResponse(
                "Política de cancelación",
                "Política de devolución",
                false,
                true,
                true,
                true,
                false,
                BigDecimal.valueOf(15.00),
                25,
                true,
                80,
                true
        );

        when(getStoreSettingsUseCase.execute(userId)).thenReturn(expected);

        ResponseEntity<StoreSettingsResponse> response = controller.getMySettings(principal);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().cancellationPolicy()).isEqualTo("Política de cancelación");
        assertThat(response.getBody().estimatedPrepTimeMinutes()).isEqualTo(25);
        verify(getStoreSettingsUseCase).execute(userId);
    }

    @Test
    @DisplayName("Debe actualizar y retornar los settings modificados")
    void shouldUpdateStoreSettings() {
        StoreSettingsRequest request = new StoreSettingsRequest(
                "Cancelación antes de 10 min",
                "Reembolso con foto",
                true,
                true,
                true,
                false,
                true,
                BigDecimal.valueOf(20.00),
                35,
                true,
                90,
                false
        );

        StoreSettingsResponse updated = new StoreSettingsResponse(
                request.cancellationPolicy(),
                request.refundPolicy(),
                request.requireCustomerDni(),
                request.allowOrderNotes(),
                request.onlinePaymentsEnabled(),
                request.cashOnDeliveryEnabled(),
                request.acceptsPosOnDelivery(),
                request.minDeliveryAmount(),
                request.estimatedPrepTimeMinutes(),
                request.orderSoundAlert(),
                request.soundVolume(),
                request.lowStockVisualAlert()
        );

        when(updateStoreSettingsUseCase.execute(userId, request)).thenReturn(updated);

        ResponseEntity<StoreSettingsResponse> response = controller.updateMySettings(principal, request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().requireCustomerDni()).isTrue();
        assertThat(response.getBody().cashOnDeliveryEnabled()).isFalse();
        assertThat(response.getBody().acceptsPosOnDelivery()).isTrue();
        verify(updateStoreSettingsUseCase).execute(userId, request);
    }
}

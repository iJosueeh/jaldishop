package com.jaldishop.backend.billing.application.service;

import com.jaldishop.backend.billing.web.dto.MerchantPayoutDestinationResponse;
import com.jaldishop.backend.billing.web.dto.UpdatePayoutDestinationRequest;
import com.jaldishop.backend.shared.security.crypto.CryptoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantBillingQueryServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private CryptoService cryptoService;

    private MerchantBillingQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new MerchantBillingQueryService(jdbcTemplate, cryptoService);
    }

    @Test
    @DisplayName("Debe insertar destino de pago cifrando datos sensibles y calculando blind index")
    void shouldInsertDestinationPassingTimestamp() {
        UUID storeId = UUID.randomUUID();
        UpdatePayoutDestinationRequest request = new UpdatePayoutDestinationRequest(
                "BANK_TRANSFER_CCI",
                "BCP",
                "194-12345678",
                "00219400123456789012",
                null,
                "Juan Pérez",
                "DNI",
                "12345678"
        );

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(storeId)))
                .thenReturn(Collections.emptyList());
        when(cryptoService.encrypt("194-12345678")).thenReturn("encAccNumber");
        when(cryptoService.encrypt("00219400123456789012")).thenReturn("encCci");
        when(cryptoService.encrypt("12345678")).thenReturn("encDocNumber");
        when(cryptoService.blindIndex("00219400123456789012")).thenReturn("blindIndexCciHash");

        MerchantPayoutDestinationResponse response = queryService.saveOrUpdateDestination(storeId, request);

        assertThat(response).isNotNull();
        assertThat(response.accountHolderName()).isEqualTo("Juan Pérez");
        assertThat(response.accountCci()).isEqualTo("00219400123456789012");

        verify(cryptoService).encrypt("194-12345678");
        verify(cryptoService).encrypt("00219400123456789012");
        verify(cryptoService).blindIndex("00219400123456789012");

        verify(jdbcTemplate).update(
                anyString(),
                any(UUID.class),
                eq(storeId),
                eq("BANK_TRANSFER_CCI"),
                eq("BCP"),
                eq("encAccNumber"),
                eq("encCci"),
                any(),
                eq("Juan Pérez"),
                eq("DNI"),
                eq("encDocNumber"),
                eq("blindIndexCciHash"),
                any(Timestamp.class),
                any(Timestamp.class)
        );
    }
}

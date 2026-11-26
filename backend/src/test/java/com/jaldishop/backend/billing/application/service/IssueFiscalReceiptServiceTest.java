package com.jaldishop.backend.billing.application.service;

import com.jaldishop.backend.billing.domain.enums.FiscalDocumentType;
import com.jaldishop.backend.billing.web.dto.FiscalReceiptResponse;
import com.jaldishop.backend.store.domain.enums.TaxIdType;
import com.jaldishop.backend.store.domain.enums.TaxRegime;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.model.StoreTaxProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueFiscalReceiptServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private IssueFiscalReceiptService service;

    @BeforeEach
    void setUp() {
        service = new IssueFiscalReceiptService(jdbcTemplate);
    }

    @Test
    @DisplayName("Tienda MYPE con RUC 20 y canIssueInvoices=true emite Factura (01) si cliente pide FACTURA con RUC válido")
    void resolveFacturaForMypeWithValidCustomerRuc() {
        Store store = Store.create(
                UUID.randomUUID(), "Gourmet SAC", "gourmet-sac", "Restaurante", "987654321",
                "Av. Lima 100", null, null, null, true, true, null, "PEN",
                new BigDecimal("18.00"), null, null, null, null, null, null,
                new StoreTaxProfile("Gourmet SAC", TaxIdType.RUC_20, "20100070970", TaxRegime.MYPE, true),
                Instant.now()
        );

        FiscalDocumentType docType = service.resolveDocumentType(store, "FACTURA", "20131312955");
        assertThat(docType).isEqualTo(FiscalDocumentType.FACTURA);
        assertThat(docType.getSunatCode()).isEqualTo("01");
        assertThat(docType.getDefaultSeries()).isEqualTo("F001");
    }

    @Test
    @DisplayName("Tienda Nuevo RUS con RUC 10 siempre emite Boleta (03) aunque soliciten Factura")
    void resolveBoletaForRusEvenIfFacturaRequested() {
        Store store = Store.create(
                UUID.randomUUID(), "Bodega Luz", "bodega-luz", "Bodega", "987654321",
                "Jr. Sol 50", null, null, null, true, false, null, "PEN",
                BigDecimal.ZERO, null, null, null, null, null, null,
                new StoreTaxProfile("Luz Quispe", TaxIdType.RUC_10, "10456789019", TaxRegime.RUS, false),
                Instant.now()
        );

        FiscalDocumentType docType = service.resolveDocumentType(store, "FACTURA", "20131312955");
        assertThat(docType).isEqualTo(FiscalDocumentType.BOLETA);
        assertThat(docType.getSunatCode()).isEqualTo("03");
        assertThat(docType.getDefaultSeries()).isEqualTo("B001");
    }

    @Test
    @DisplayName("Tienda en formalización con DNI sin RUC emite Nota de Venta (00)")
    void resolveNotaVentaForDniStore() {
        Store store = Store.create(
                UUID.randomUUID(), "Postres Ana", "postres-ana", "Repostería", "987654321",
                "Calle 1", null, null, null, true, false, null, "PEN",
                BigDecimal.ZERO, null, null, null, null, null, null,
                new StoreTaxProfile("Ana Torres", TaxIdType.DNI, "12345678", TaxRegime.RUS, false),
                Instant.now()
        );

        FiscalDocumentType docType = service.resolveDocumentType(store, "BOLETA", "87654321");
        assertThat(docType).isEqualTo(FiscalDocumentType.NOTA_VENTA);
        assertThat(docType.getSunatCode()).isEqualTo("00");
        assertThat(docType.getDefaultSeries()).isEqualTo("NV01");
    }

    @Test
    @DisplayName("issueForOrder retorna comprobante existente de forma idempotente")
    void issueForOrderIdempotent() {
        UUID orderId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        FiscalReceiptResponse existing = new FiscalReceiptResponse(
                UUID.randomUUID(), orderId, UUID.randomUUID(), storeId,
                "03", "Boleta de Venta", "B001", 1L, "B001-00000001",
                "RUC_10", "10456789019", "Luz Quispe", "RUS",
                "DNI", "12345678", "Carlos",
                new BigDecimal("50.00"), new BigDecimal("0.00"), new BigDecimal("0.00"),
                new BigDecimal("50.00"), "PEN", "MERCADO_PAGO", "mp-123", Instant.now()
        );

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(orderId)))
                .thenReturn(List.of(existing));

        Store store = Store.create(
                UUID.randomUUID(), "Bodega Luz", "bodega-luz", "Bodega", "987654321",
                "Jr. Sol 50", null, null, null, true, false, null, "PEN",
                BigDecimal.ZERO, null, null, null, null, null, null,
                new StoreTaxProfile("Luz Quispe", TaxIdType.RUC_10, "10456789019", TaxRegime.RUS, false),
                Instant.now()
        );

        FiscalReceiptResponse result = service.issueForOrder(
                orderId, UUID.randomUUID(), UUID.randomUUID(), store,
                new BigDecimal("50.00"), "PEN", "Carlos",
                "BOLETA", "DNI", "12345678", null
        );

        assertThat(result.fullNumber()).isEqualTo("B001-00000001");
        assertThat(result.documentTypeCode()).isEqualTo("03");
    }
}

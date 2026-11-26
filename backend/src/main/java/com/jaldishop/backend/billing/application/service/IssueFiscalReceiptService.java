package com.jaldishop.backend.billing.application.service;

import com.jaldishop.backend.billing.domain.enums.FiscalDocumentType;
import com.jaldishop.backend.billing.web.dto.FiscalReceiptResponse;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.enums.TaxIdType;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.validation.SunatRucValidator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Emite comprobantes fiscales SUNAT (01 Factura, 03 Boleta, 00 Nota de Venta)
 * de forma idempotente por orden, asignando serie y correlativo atómico por tienda.
 */
@Service
public class IssueFiscalReceiptService {

    private final JdbcTemplate jdbcTemplate;

    public IssueFiscalReceiptService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public FiscalReceiptResponse issueForOrder(
            UUID orderId,
            UUID paymentId,
            UUID userId,
            Store store,
            BigDecimal totalAmount,
            String currency,
            String customerName,
            String requestedReceiptType,
            String requestedCustomerDocType,
            String requestedCustomerDocNumber,
            String requestedCustomerLegalName
    ) {
        Optional<FiscalReceiptResponse> existing = findByOrderId(orderId);
        if (existing.isPresent()) {
            return existing.get();
        }

        FiscalDocumentType docType = resolveDocumentType(store, requestedReceiptType, requestedCustomerDocNumber);
        long correlative = nextCorrelative(store.getId(), docType);
        String series = docType.getDefaultSeries();
        String fullNumber = series + "-" + String.format("%08d", correlative);

        String emitterLegalName = (store.getLegalBusinessName() != null && !store.getLegalBusinessName().isBlank())
                ? store.getLegalBusinessName()
                : store.getName();
        String emitterTaxIdType = store.getTaxIdType() != null ? store.getTaxIdType().name() : TaxIdType.DNI.name();
        String emitterTaxRegime = store.getTaxRegime() != null ? store.getTaxRegime().name() : "RUS";

        String resolvedCustomerDocType = resolveCustomerDocType(docType, requestedCustomerDocType, requestedCustomerDocNumber);
        String resolvedCustomerDocNumber = (requestedCustomerDocNumber != null && !requestedCustomerDocNumber.isBlank())
                ? requestedCustomerDocNumber.trim()
                : null;
        String resolvedCustomerName = (docType == FiscalDocumentType.FACTURA && requestedCustomerLegalName != null && !requestedCustomerLegalName.isBlank())
                ? requestedCustomerLegalName.trim()
                : (customerName != null && !customerName.isBlank() ? customerName.trim() : "Cliente General");

        BigDecimal effectiveTotal = totalAmount != null ? totalAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal igvRate = resolveIgvRate(store);
        BigDecimal taxableAmount;
        BigDecimal igvAmount;

        if (igvRate.compareTo(BigDecimal.ZERO) > 0 && effectiveTotal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal divisor = BigDecimal.ONE.add(igvRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
            taxableAmount = effectiveTotal.divide(divisor, 2, RoundingMode.HALF_UP);
            igvAmount = effectiveTotal.subtract(taxableAmount).setScale(2, RoundingMode.HALF_UP);
        } else {
            taxableAmount = effectiveTotal;
            igvAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        String providerPaymentId = findApprovedProviderPaymentId(paymentId);
        String paymentProvider = paymentId != null ? "MERCADO_PAGO" : "DIRECT";

        UUID receiptId = UUID.randomUUID();
        Instant issuedAt = Instant.now();

        jdbcTemplate.update(
                """
                INSERT INTO fiscal_receipts (
                    id, order_id, payment_id, store_id, user_id,
                    document_type_code, series, correlative, full_number,
                    emitter_tax_id_type, emitter_tax_id_number, emitter_legal_name, emitter_tax_regime,
                    customer_doc_type, customer_doc_number, customer_name,
                    taxable_amount, igv_rate, igv_amount, total_amount, currency,
                    payment_provider, provider_payment_id, issued_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (order_id) DO NOTHING
                """,
                receiptId,
                orderId,
                paymentId,
                store.getId(),
                userId,
                docType.getSunatCode(),
                series,
                correlative,
                fullNumber,
                emitterTaxIdType,
                store.getTaxIdNumber(),
                emitterLegalName,
                emitterTaxRegime,
                resolvedCustomerDocType,
                resolvedCustomerDocNumber,
                resolvedCustomerName,
                taxableAmount,
                igvRate,
                igvAmount,
                effectiveTotal,
                currency != null && !currency.isBlank() ? currency : "PEN",
                paymentProvider,
                providerPaymentId,
                Timestamp.from(issuedAt)
        );

        return findByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException("No se pudo recuperar el comprobante generado para la orden " + orderId));
    }

    @Transactional(readOnly = true)
    public Optional<FiscalReceiptResponse> findByOrderId(UUID orderId) {
        List<FiscalReceiptResponse> list = jdbcTemplate.query(
                "SELECT * FROM fiscal_receipts WHERE order_id = ?",
                RECEIPT_ROW_MAPPER,
                orderId
        );
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Transactional(readOnly = true)
    public FiscalReceiptResponse getForOrderAuthorized(UUID orderId, UUID requesterUserId) {
        FiscalReceiptResponse receipt = findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("RECEIPT_NOT_FOUND", "No existe un comprobante emitido para este pedido."));

        Boolean isCustomerOwner = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM fiscal_receipts WHERE order_id = ? AND user_id = ?)",
                Boolean.class,
                orderId,
                requesterUserId
        );
        if (Boolean.TRUE.equals(isCustomerOwner)) {
            return receipt;
        }

        Boolean isStoreOwner = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM stores WHERE id = ? AND owner_user_id = ?)",
                Boolean.class,
                receipt.storeId(),
                requesterUserId
        );
        if (Boolean.TRUE.equals(isStoreOwner)) {
            return receipt;
        }

        throw new ResourceNotFoundException("RECEIPT_NOT_FOUND", "No existe un comprobante emitido para este pedido.");
    }

    @Transactional(readOnly = true)
    public List<FiscalReceiptResponse> listByStore(UUID storeId) {
        return jdbcTemplate.query(
                "SELECT * FROM fiscal_receipts WHERE store_id = ? ORDER BY issued_at DESC LIMIT 100",
                RECEIPT_ROW_MAPPER,
                storeId
        );
    }

    public FiscalDocumentType resolveDocumentType(Store store, String requestedReceiptType, String customerDocNumber) {
        boolean storeHasRuc = (store.getTaxIdType() == TaxIdType.RUC_10 || store.getTaxIdType() == TaxIdType.RUC_20)
                && store.getTaxIdNumber() != null
                && !store.getTaxIdNumber().isBlank();

        if ("FACTURA".equalsIgnoreCase(requestedReceiptType)
                && store.isCanIssueInvoices()
                && storeHasRuc
                && SunatRucValidator.isValidRuc(customerDocNumber)) {
            return FiscalDocumentType.FACTURA;
        }

        if (storeHasRuc) {
            return FiscalDocumentType.BOLETA;
        }

        return FiscalDocumentType.NOTA_VENTA;
    }

    private long nextCorrelative(UUID storeId, FiscalDocumentType docType) {
        Long next = jdbcTemplate.queryForObject(
                """
                INSERT INTO store_receipt_sequences (store_id, document_type_code, series, last_correlative, updated_at)
                VALUES (?, ?, ?, 1, CURRENT_TIMESTAMP)
                ON CONFLICT (store_id, document_type_code)
                DO UPDATE SET last_correlative = store_receipt_sequences.last_correlative + 1,
                              updated_at = CURRENT_TIMESTAMP
                RETURNING last_correlative
                """,
                Long.class,
                storeId,
                docType.getSunatCode(),
                docType.getDefaultSeries()
        );
        return next != null ? next : 1L;
    }

    private BigDecimal resolveIgvRate(Store store) {
        if (store.getTaxRegime() != null && (store.getTaxRegime().name().equals("RUS") || store.getTaxRegime().name().equals("NONE"))) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return store.getTaxRate() != null
                ? store.getTaxRate().setScale(2, RoundingMode.HALF_UP)
                : new BigDecimal("18.00");
    }

    private String resolveCustomerDocType(FiscalDocumentType docType, String requestedDocType, String docNumber) {
        if (docType == FiscalDocumentType.FACTURA) {
            return "RUC";
        }
        if (requestedDocType != null && !requestedDocType.isBlank()) {
            return requestedDocType.trim().toUpperCase();
        }
        if (docNumber != null && docNumber.trim().length() == 11) {
            return "RUC";
        }
        if (docNumber != null && docNumber.trim().length() == 8) {
            return "DNI";
        }
        return "NONE";
    }

    private String findApprovedProviderPaymentId(UUID paymentId) {
        if (paymentId == null) {
            return null;
        }
        List<String> ids = jdbcTemplate.queryForList(
                """
                SELECT provider_payment_id
                FROM payment_attempts
                WHERE payment_id = ? AND status = 'APPROVED' AND provider_payment_id IS NOT NULL
                ORDER BY attempt_number DESC
                LIMIT 1
                """,
                String.class,
                paymentId
        );
        return ids.isEmpty() ? null : ids.get(0);
    }

    private static final RowMapper<FiscalReceiptResponse> RECEIPT_ROW_MAPPER = (rs, rowNum) -> {
        String code = rs.getString("document_type_code");
        FiscalDocumentType docType = FiscalDocumentType.fromSunatCode(code);
        return new FiscalReceiptResponse(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("order_id")),
                rs.getString("payment_id") != null ? UUID.fromString(rs.getString("payment_id")) : null,
                UUID.fromString(rs.getString("store_id")),
                code,
                docType.getDisplayName(),
                rs.getString("series"),
                rs.getLong("correlative"),
                rs.getString("full_number"),
                rs.getString("emitter_tax_id_type"),
                rs.getString("emitter_tax_id_number"),
                rs.getString("emitter_legal_name"),
                rs.getString("emitter_tax_regime"),
                rs.getString("customer_doc_type"),
                rs.getString("customer_doc_number"),
                rs.getString("customer_name"),
                rs.getBigDecimal("taxable_amount"),
                rs.getBigDecimal("igv_rate"),
                rs.getBigDecimal("igv_amount"),
                rs.getBigDecimal("total_amount"),
                rs.getString("currency"),
                rs.getString("payment_provider"),
                rs.getString("provider_payment_id"),
                rs.getTimestamp("issued_at").toInstant()
        );
    };
}

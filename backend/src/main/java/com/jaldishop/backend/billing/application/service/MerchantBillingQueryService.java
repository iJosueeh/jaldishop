package com.jaldishop.backend.billing.application.service;

import com.jaldishop.backend.billing.web.dto.MerchantBillingSummaryResponse;
import com.jaldishop.backend.billing.web.dto.MerchantPayoutBatchResponse;
import com.jaldishop.backend.billing.web.dto.MerchantPayoutDestinationResponse;
import com.jaldishop.backend.billing.web.dto.UpdatePayoutDestinationRequest;
import com.jaldishop.backend.shared.security.crypto.CryptoService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class MerchantBillingQueryService {

    private final JdbcTemplate jdbcTemplate;
    private final CryptoService cryptoService;

    public MerchantBillingQueryService(JdbcTemplate jdbcTemplate, CryptoService cryptoService) {
        this.jdbcTemplate = jdbcTemplate;
        this.cryptoService = cryptoService;
    }

    @Transactional(readOnly = true)
    public MerchantBillingSummaryResponse getBillingSummary(UUID storeId) {
        BigDecimal availableBalance = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(credit_amount - debit_amount), 0) FROM merchant_payout_ledgers WHERE store_id = ?",
                BigDecimal.class,
                storeId
        );
        if (availableBalance == null) {
            availableBalance = BigDecimal.ZERO;
        }

        BigDecimal lockedBalance = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(gross_amount), 0) FROM merchant_payout_batches WHERE store_id = ? AND status IN ('PENDING', 'PROCESSING')",
                BigDecimal.class,
                storeId
        );
        if (lockedBalance == null) {
            lockedBalance = BigDecimal.ZERO;
        }

        BigDecimal totalPaidOut = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(net_disbursed_amount), 0) FROM merchant_payout_batches WHERE store_id = ? AND status = 'PAID'",
                BigDecimal.class,
                storeId
        );
        if (totalPaidOut == null) {
            totalPaidOut = BigDecimal.ZERO;
        }

        MerchantPayoutDestinationResponse destination = getPrimaryDestination(storeId);

        return new MerchantBillingSummaryResponse(
                storeId,
                availableBalance,
                lockedBalance,
                totalPaidOut,
                new BigDecimal("50.00"),
                "PEN",
                destination,
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public List<MerchantPayoutBatchResponse> getPayoutBatches(UUID storeId) {
        String sql = """
                SELECT id, store_id, status, gross_amount, banking_fee, net_disbursed_amount, 
                       currency, bank_operation_number, scheduled_for, processed_at, created_at
                FROM merchant_payout_batches
                WHERE store_id = ?
                ORDER BY created_at DESC
                LIMIT 50
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new MerchantPayoutBatchResponse(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("store_id")),
                rs.getString("status"),
                rs.getBigDecimal("gross_amount"),
                rs.getBigDecimal("banking_fee"),
                rs.getBigDecimal("net_disbursed_amount"),
                rs.getString("currency"),
                rs.getString("bank_operation_number"),
                rs.getObject("scheduled_for", LocalDate.class),
                rs.getTimestamp("processed_at") != null ? rs.getTimestamp("processed_at").toInstant() : null,
                rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : null
        ), storeId);
    }

    @Transactional(readOnly = true)
    public MerchantPayoutDestinationResponse getPrimaryDestination(UUID storeId) {
        String sql = """
                SELECT id, store_id, payout_channel, bank_name, account_number, account_cci, 
                       phone_number, account_holder_name, account_holder_doc_type, account_holder_doc_number, 
                       is_primary, is_verified, updated_at
                FROM merchant_payout_destinations
                WHERE store_id = ? AND is_primary = true
                LIMIT 1
                """;

        List<MerchantPayoutDestinationResponse> list = jdbcTemplate.query(sql, new DestinationRowMapper(cryptoService), storeId);
        return list.isEmpty() ? null : list.get(0);
    }

    @Transactional
    public MerchantPayoutDestinationResponse saveOrUpdateDestination(UUID storeId, UpdatePayoutDestinationRequest req) {
        MerchantPayoutDestinationResponse existing = getPrimaryDestination(storeId);
        Instant now = Instant.now();
        Timestamp nowTs = Timestamp.from(now);

        String encAccountNumber = (cryptoService != null && req.accountNumber() != null) ? cryptoService.encrypt(req.accountNumber()) : req.accountNumber();
        String encCci = (cryptoService != null && req.accountCci() != null) ? cryptoService.encrypt(req.accountCci()) : req.accountCci();
        String encPhone = (cryptoService != null && req.phoneNumber() != null) ? cryptoService.encrypt(req.phoneNumber()) : req.phoneNumber();
        String encDocNumber = (cryptoService != null && req.accountHolderDocNumber() != null) ? cryptoService.encrypt(req.accountHolderDocNumber()) : req.accountHolderDocNumber();
        String blindIndexCci = (cryptoService != null && req.accountCci() != null) ? cryptoService.blindIndex(req.accountCci()) : null;

        if (existing != null) {
            jdbcTemplate.update("""
                    UPDATE merchant_payout_destinations
                    SET payout_channel = ?, bank_name = ?, account_number = ?, account_cci = ?, 
                        phone_number = ?, account_holder_name = ?, account_holder_doc_type = ?, 
                        account_holder_doc_number = ?, account_cci_blind_index = ?, updated_at = ?
                    WHERE id = ?
                    """,
                    req.payoutChannel(),
                    req.bankName(),
                    encAccountNumber,
                    encCci,
                    encPhone,
                    req.accountHolderName(),
                    req.accountHolderDocType(),
                    encDocNumber,
                    blindIndexCci,
                    nowTs,
                    existing.id()
            );
            return new MerchantPayoutDestinationResponse(
                    existing.id(),
                    storeId,
                    req.payoutChannel(),
                    req.bankName(),
                    req.accountNumber(),
                    req.accountCci(),
                    req.phoneNumber(),
                    req.accountHolderName(),
                    req.accountHolderDocType(),
                    req.accountHolderDocNumber(),
                    existing.isPrimary(),
                    existing.isVerified(),
                    now
            );
        } else {
            UUID newId = UUID.randomUUID();
            jdbcTemplate.update("""
                    INSERT INTO merchant_payout_destinations
                    (id, store_id, payout_channel, bank_name, account_number, account_cci, 
                     phone_number, account_holder_name, account_holder_doc_type, account_holder_doc_number, 
                     account_cci_blind_index, is_primary, is_verified, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, true, false, ?, ?)
                    """,
                    newId,
                    storeId,
                    req.payoutChannel(),
                    req.bankName(),
                    encAccountNumber,
                    encCci,
                    encPhone,
                    req.accountHolderName(),
                    req.accountHolderDocType(),
                    encDocNumber,
                    blindIndexCci,
                    nowTs,
                    nowTs
            );
            return new MerchantPayoutDestinationResponse(
                    newId,
                    storeId,
                    req.payoutChannel(),
                    req.bankName(),
                    req.accountNumber(),
                    req.accountCci(),
                    req.phoneNumber(),
                    req.accountHolderName(),
                    req.accountHolderDocType(),
                    req.accountHolderDocNumber(),
                    true,
                    false,
                    now
            );
        }
    }

    private static class DestinationRowMapper implements RowMapper<MerchantPayoutDestinationResponse> {
        private final CryptoService cryptoService;

        public DestinationRowMapper(CryptoService cryptoService) {
            this.cryptoService = cryptoService;
        }

        @Override
        public MerchantPayoutDestinationResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            String rawAccNumber = rs.getString("account_number");
            String rawCci = rs.getString("account_cci");
            String rawPhone = rs.getString("phone_number");
            String rawDocNumber = rs.getString("account_holder_doc_number");

            return new MerchantPayoutDestinationResponse(
                    UUID.fromString(rs.getString("id")),
                    UUID.fromString(rs.getString("store_id")),
                    rs.getString("payout_channel"),
                    rs.getString("bank_name"),
                    cryptoService != null ? cryptoService.decrypt(rawAccNumber) : rawAccNumber,
                    cryptoService != null ? cryptoService.decrypt(rawCci) : rawCci,
                    cryptoService != null ? cryptoService.decrypt(rawPhone) : rawPhone,
                    rs.getString("account_holder_name"),
                    rs.getString("account_holder_doc_type"),
                    cryptoService != null ? cryptoService.decrypt(rawDocNumber) : rawDocNumber,
                    rs.getBoolean("is_primary"),
                    rs.getBoolean("is_verified"),
                    updatedAt != null ? updatedAt.toInstant() : null
            );
        }
    }
}

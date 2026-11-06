package com.jaldishop.backend.payment.application.investigation;

import com.jaldishop.backend.payment.application.gateway.MercadoPagoGateway;
import com.jaldishop.backend.payment.application.gateway.dto.OrderQueryResult;
import com.jaldishop.backend.payment.domain.enums.RemoteOrderCreationState;
import com.jaldishop.backend.payment.domain.enums.PaymentProvider;
import com.jaldishop.backend.payment.domain.model.Payment;
import com.jaldishop.backend.payment.domain.model.PaymentAttempt;
import com.jaldishop.backend.payment.domain.repository.PaymentAttemptRepository;
import com.jaldishop.backend.payment.domain.repository.PaymentRepository;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoException;
import com.jaldishop.backend.payment.infrastructure.client.mercadopago.MercadoPagoProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;
import java.util.UUID;

@Service
public class ManualOrderInvestigationService {
    public enum Outcome { VERIFIED, INCONCLUSIVE, CONTRADICTION, TECHNICAL_ERROR }
    public record Result(Outcome outcome, String reason) {}
    private final PaymentAttemptRepository attempts;
    private final PaymentRepository payments;
    private final MercadoPagoGateway gateway;
    private final MercadoPagoProperties properties;
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final TransactionTemplate transaction;

    public ManualOrderInvestigationService(PaymentAttemptRepository attempts, PaymentRepository payments,
            MercadoPagoGateway gateway, MercadoPagoProperties properties, JdbcTemplate jdbc,
            ObjectMapper mapper, PlatformTransactionManager manager) {
        this.attempts = attempts;
        this.payments = payments;
        this.gateway = gateway;
        this.properties = properties;
        this.jdbc = jdbc;
        this.mapper = mapper;
        transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    // Suspend even a caller's transaction before any provider request.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Result investigate(UUID attemptId) {
        try { return performInvestigation(attemptId); }
        catch (RuntimeException ex) { return new Result(Outcome.TECHNICAL_ERROR, "LOCAL_FAILURE"); }
    }

    private Result performInvestigation(UUID attemptId) {
        PaymentAttempt initial = attempts.findById(attemptId).orElse(null);
        if (initial == null) return new Result(Outcome.INCONCLUSIVE, "ATTEMPT_NOT_FOUND");
        if (!"ORDERS".equals(initial.getCheckoutMode()))
            return new Result(Outcome.CONTRADICTION, "ORDERS_REQUIRED");
        if (initial.getRemoteOrderCreationState() != RemoteOrderCreationState.UNKNOWN)
            return audit(initial, new Result(Outcome.INCONCLUSIVE, "NOT_UNKNOWN"));
        if (initial.getProviderOrderId() == null || initial.getProviderOrderId().isBlank())
            return audit(initial, new Result(Outcome.INCONCLUSIVE, "NO_UNAMBIGUOUS_ORDER_ID"));
        OrderQueryResult remote;
        try {
            remote = gateway.queryOrder(initial.getProviderOrderId());
        } catch (MercadoPagoException ex) {
            return audit(initial, new Result(ex.getStatusCode() == 404 ? Outcome.INCONCLUSIVE : Outcome.TECHNICAL_ERROR,
                    ex.getStatusCode() == 404 ? "REMOTE_NOT_FOUND" : "REMOTE_FAILURE"));
        } catch (RuntimeException ex) {
            return audit(initial, new Result(Outcome.TECHNICAL_ERROR, "REMOTE_FAILURE"));
        }
        return transaction.execute(status -> {
            // Match webhook writes: lock the expected payment before reloading its attempt.
            Payment payment = payments.findByIdForUpdate(initial.getPaymentId()).orElse(null);
            PaymentAttempt current = attempts.findByIdForUpdate(attemptId).orElseThrow();
            Result result = validate(initial, current, payment, remote);
            if (result.outcome() == Outcome.VERIFIED && remote.checkoutUrl() != null
                    && !remote.checkoutUrl().isBlank()) {
                current.recordOrder(remote.orderId(), remote.checkoutUrl());
                attempts.save(current);
            }
            writeAudit(current, result);
            // Save only the locked current attempt; no financial transition.
            return result;
        });
    }

    private Result validate(PaymentAttempt initial, PaymentAttempt current, Payment payment, OrderQueryResult remote) {
        if (payment == null || !Objects.equals(initial.getPaymentId(), current.getPaymentId())
                || !Objects.equals(payment.getId(), current.getPaymentId())
                || !"ORDERS".equals(current.getCheckoutMode())
                || current.getProvider() != PaymentProvider.MERCADO_PAGO
                || !Objects.equals(initial.getProviderOrderId(), current.getProviderOrderId())
                || remote == null || !Objects.equals(current.getProviderOrderId(), remote.orderId()))
            return new Result(Outcome.CONTRADICTION, "IDENTITY_CHANGED");
        var owner = attempts.findByProviderOrderId(remote.orderId());
        if (owner.isPresent() && !owner.get().getId().equals(current.getId()))
            return new Result(Outcome.CONTRADICTION, "ORDER_ALREADY_OWNED");
        var bodies = jdbc.queryForList("""
                SELECT request_body FROM checkout_creation_requests
                WHERE payment_attempt_id = ? AND checkout_mode = 'ORDERS' AND idempotency_key = ? AND format_version = 1
                """, String.class, current.getId(), current.getIdempotencyKey());
        if (bodies.size() != 1) return new Result(Outcome.INCONCLUSIVE, "SNAPSHOT_UNAVAILABLE");
        tools.jackson.databind.JsonNode snapshot;
        try { snapshot = mapper.readTree(bodies.getFirst()); }
        catch (RuntimeException ex) { return new Result(Outcome.TECHNICAL_ERROR, "INVALID_SNAPSHOT"); }
        String reference = snapshot.path("external_reference").asText("");
        if (!reference.equals(payment.getId().toString()) || !reference.equals(remote.externalReference()))
            return new Result(Outcome.CONTRADICTION, "REFERENCE_MISMATCH");
        java.math.BigDecimal sent;
        try { sent = new java.math.BigDecimal(snapshot.path("total_amount").asText()); }
        catch (RuntimeException ex) { return new Result(Outcome.INCONCLUSIVE, "SNAPSHOT_AMOUNT_UNAVAILABLE"); }
        if (remote.totalAmount() == null || payment.getAmount() == null)
            return new Result(Outcome.INCONCLUSIVE, "AMOUNT_UNAVAILABLE");
        if (sent.compareTo(payment.getAmount()) != 0 || sent.compareTo(remote.totalAmount()) != 0)
            return new Result(Outcome.CONTRADICTION, "AMOUNT_MISMATCH");
        if (properties.getExpectedCollectorId() != null && remote.collectorId() != null
                && !properties.getExpectedCollectorId().equals(remote.collectorId())
                || properties.getExpectedLiveMode() != null && remote.liveMode() != null
                && !properties.getExpectedLiveMode().equals(remote.liveMode()))
            return new Result(Outcome.CONTRADICTION, "ACCOUNT_OR_ENVIRONMENT_MISMATCH");
        if (current.getProviderPaymentId() != null && remote.payments().stream()
                .noneMatch(item -> current.getProviderPaymentId().equals(item.providerPaymentId())))
            return new Result(Outcome.CONTRADICTION, "LOCAL_PAYMENT_NOT_IN_ORDER");
        boolean currencyFound = false;
        for (var item : remote.payments()) {
            if (item.currency() != null) currencyFound = true;
            if (item.currency() != null && !item.currency().equals(payment.getCurrency()))
                return new Result(Outcome.CONTRADICTION, "CURRENCY_MISMATCH");
            var paymentOwner = attempts.findByProviderAndProviderPaymentId(current.getProvider(), item.providerPaymentId());
            if (paymentOwner.isPresent() && !paymentOwner.get().getId().equals(current.getId()))
                return new Result(Outcome.CONTRADICTION, "PAYMENT_ALREADY_OWNED");
        }
        if (!currencyFound) return new Result(Outcome.INCONCLUSIVE, "CURRENCY_UNAVAILABLE");
        if (properties.getExpectedCollectorId() != null && remote.collectorId() == null
                || properties.getExpectedLiveMode() != null && remote.liveMode() == null)
            return new Result(Outcome.INCONCLUSIVE, "ACCOUNT_OR_ENVIRONMENT_UNAVAILABLE");
        if (current.getRemoteOrderCreationState() != RemoteOrderCreationState.UNKNOWN)
            return new Result(Outcome.INCONCLUSIVE, "CONCURRENT_CREATION_EVIDENCE");
        return new Result(Outcome.VERIFIED, "IDENTITY_VERIFIED");
    }

    private Result audit(PaymentAttempt attempt, Result result) {
        return transaction.execute(status -> {
            Payment payment = payments.findByIdForUpdate(attempt.getPaymentId()).orElse(null);
            PaymentAttempt current = attempts.findByIdForUpdate(attempt.getId()).orElseThrow();
            Result checked = payment == null || !Objects.equals(payment.getId(), current.getPaymentId())
                    ? new Result(Outcome.CONTRADICTION, "IDENTITY_CHANGED") : result;
            writeAudit(current, checked);
            return checked;
        });
    }

    private void writeAudit(PaymentAttempt attempt, Result result) {
        jdbc.update("""
                INSERT INTO remote_order_creation_events
                (payment_attempt_id, previous_state, new_state, evidence_code)
                VALUES (?, ?, ?, ?)
                """, attempt.getId(), attempt.getRemoteOrderCreationState().name(),
                attempt.getRemoteOrderCreationState().name(), "MANUAL_" + result.reason());
    }
}

package com.jaldishop.backend.payment.application.gateway;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/** Identifica el tipo de referencia sin resolver ni inferir su propietario. */
public record ExternalPaymentReference(Kind kind, UUID id) {
    public enum Kind { PAYMENT, PAYMENT_ATTEMPT }

    private static final String ATTEMPT_PREFIX = "pa1_";
    private static final Pattern UUID_FORMAT = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    public static Optional<ExternalPaymentReference> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        boolean attempt = value.startsWith(ATTEMPT_PREFIX);
        String uuid = attempt ? value.substring(ATTEMPT_PREFIX.length()) : value;
        if (!UUID_FORMAT.matcher(uuid).matches()) {
            return Optional.empty();
        }
        return Optional.of(new ExternalPaymentReference(
                attempt ? Kind.PAYMENT_ATTEMPT : Kind.PAYMENT, UUID.fromString(uuid)));
    }
}

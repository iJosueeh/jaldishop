package com.jaldishop.backend.payment.application.gateway;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalPaymentReferenceTest {
    private static final UUID ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @Test
    void parsesLegacyPaymentAndVersionedAttempt() {
        assertThat(ExternalPaymentReference.parse(ID.toString()))
                .contains(new ExternalPaymentReference(ExternalPaymentReference.Kind.PAYMENT, ID));
        assertThat(ExternalPaymentReference.parse("pa1_" + ID.toString().toUpperCase()))
                .contains(new ExternalPaymentReference(ExternalPaymentReference.Kind.PAYMENT_ATTEMPT, ID));
        assertThat(("pa1_" + ID).length()).isEqualTo(40);
    }

    @Test
    void rejectsUnknownInvalidAndNonCanonicalReferences() {
        for (String value : new String[] {null, "", " ", "pa1_", "pa2_" + ID, "PA1_" + ID,
                "pa1_pa1_" + ID, " " + ID, ID + " ", ID + "0", "1-1-1-1-1",
                "123e4567-e89b-12d3-a456-42661417400g", "pa1_" + "0".repeat(60)}) {
            assertThat(ExternalPaymentReference.parse(value)).as("reference: %s", value).isEmpty();
        }
    }
}

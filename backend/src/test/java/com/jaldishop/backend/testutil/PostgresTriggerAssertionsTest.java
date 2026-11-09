package com.jaldishop.backend.testutil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;

class PostgresTriggerAssertionsTest {
    @ParameterizedTest
    @ValueSource(strings = {"P4201", "P4202"})
    void unrelatedRateLimitFailureCannotSatisfyTriggerAssertionAndIsReported(String sqlState) {
        var failure = new com.jaldishop.backend.shared.security.ratelimit.RateLimitExceeded(60);
        var assertion = assertThrows(AssertionError.class,
                () -> PostgresTriggerAssertions.assertFailure(failure, sqlState, "simulated trigger failure"));
        assertThat(assertion).hasMessageContaining(sqlState).hasMessageContaining("RateLimitExceeded");
    }

    @Test
    void acceptsOnlyTheExpectedSqlStateAndMessageInTheCauseChain() {
        var expected = new IllegalStateException(new SQLException("simulated commit failure", "P4201"));
        PostgresTriggerAssertions.assertFailure(expected, "P4201", "simulated commit failure");
        assertThrows(AssertionError.class, () -> PostgresTriggerAssertions.assertFailure(
                new IllegalStateException("unrelated"), "P4201", "simulated commit failure"));
        assertThrows(AssertionError.class, () -> PostgresTriggerAssertions.assertFailure(
                new SQLException("simulated commit failure", "P4202"), "P4201", "simulated commit failure"));
        assertThrows(AssertionError.class, () -> PostgresTriggerAssertions.assertFailure(
                new SQLException("unrelated", "P4201"), "P4201", "simulated commit failure"));
    }
}

package com.jaldishop.backend.testutil;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertThrows;

class PostgresTriggerAssertionsTest {
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

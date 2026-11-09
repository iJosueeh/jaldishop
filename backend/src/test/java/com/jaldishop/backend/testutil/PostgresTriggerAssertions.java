package com.jaldishop.backend.testutil;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

public final class PostgresTriggerAssertions {
    private PostgresTriggerAssertions() {}

    public static void assertFailure(Throwable failure, String sqlState, String message) {
        Throwable cause = failure;
        while (cause != null && !(cause instanceof SQLException sqlException
                && sqlState.equals(sqlException.getSQLState())
                && sqlException.getMessage() != null && sqlException.getMessage().contains(message))) {
            cause = cause.getCause();
        }
        assertThat(cause).as("PostgreSQL trigger error %s: %s; actual failure: %s",
                sqlState, message, failure).isNotNull();
    }
}

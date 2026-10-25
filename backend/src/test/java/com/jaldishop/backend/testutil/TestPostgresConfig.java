package com.jaldishop.backend.testutil;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Connection settings for the dedicated local PostgreSQL test database. */
public final class TestPostgresConfig {

    private static final Pattern TEST_URL = Pattern.compile(
            "^jdbc:postgresql://(localhost|127\\.0\\.0\\.1|\\[::1\\]):([1-9][0-9]{0,4})/jaldishop_test$");

    private final String url;
    private final String username;
    private final String password;
    private final boolean validateOnMigrate;

    private TestPostgresConfig(String url, String username, String password, boolean validateOnMigrate) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.validateOnMigrate = validateOnMigrate;
    }

    public static TestPostgresConfig fromEnvironment() {
        return fromValues(
                System.getenv("DB_URL"),
                System.getenv("DB_USERNAME"),
                System.getenv("DB_PASSWORD"),
                System.getenv("FLYWAY_VALIDATE_ON_MIGRATE"));
    }

    static TestPostgresConfig fromValues(String url, String username, String password) {
        return fromValues(url, username, password, null);
    }

    static TestPostgresConfig fromValues(String url, String username, String password, String flywayValidateOnMigrate) {
        if (url == null || username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException("DB_URL, DB_USERNAME and DB_PASSWORD must be set in the process environment for integration tests.");
        }

        Matcher match = TEST_URL.matcher(url);
        if (!match.matches() || Integer.parseInt(match.group(2)) > 65535) {
            throw new IllegalStateException("Integration tests require a loopback PostgreSQL URL with an explicit port and database jaldishop_test, without parameters.");
        }

        if (match.group(1).equals("localhost")) {
            try {
                for (InetAddress address : InetAddress.getAllByName("localhost")) {
                    if (!address.isLoopbackAddress()) {
                        throw new IllegalStateException("localhost must resolve only to loopback addresses for integration tests.");
                    }
                }
            } catch (UnknownHostException e) {
                throw new IllegalStateException("localhost must resolve to a loopback address for integration tests.", e);
            }
        }

        boolean validate = parseValidateOnMigrate(flywayValidateOnMigrate);
        return new TestPostgresConfig(url, username, password, validate);
    }

    public static boolean flywayValidateOnMigrate() {
        return parseValidateOnMigrate(System.getenv("FLYWAY_VALIDATE_ON_MIGRATE"));
    }

    static boolean parseValidateOnMigrate(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return false;
        }
        String trimmed = rawValue.trim();
        if (trimmed.equalsIgnoreCase("true")) {
            return true;
        }
        if (trimmed.equalsIgnoreCase("false")) {
            return false;
        }
        throw new IllegalStateException("FLYWAY_VALIDATE_ON_MIGRATE must be either 'true' or 'false' (case-insensitive) if set, but was: " + rawValue);
    }

    public String url() {
        return url;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public boolean validateOnMigrate() {
        return validateOnMigrate;
    }
}

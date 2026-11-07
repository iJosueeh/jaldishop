package com.jaldishop.backend.testutil;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;

/** Connection settings for the dedicated local PostgreSQL test database. */
public final class TestPostgresConfig {

    private static final Pattern TEST_URL = Pattern.compile(
            "^jdbc:postgresql://(localhost|127\\.0\\.0\\.1|\\[::1\\]):([1-9][0-9]{0,4})/(jaldishop_integration_test|jaldishop_fase2a_test)$");

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
        return fromEnvironment(System.getenv(), false);
    }

    public static TestPostgresConfig fromEnvironmentForPhase2a() {
        return fromEnvironment(System.getenv(), true);
    }

    static TestPostgresConfig fromEnvironment(Map<String, String> environment, boolean phase2a) {
        String urlVariable = phase2a ? "PHASE2A_DB_URL" : "DB_URL";
        return fromValuesFor(
                environment.get(urlVariable),
                environment.get("DB_USERNAME"),
                environment.get("DB_PASSWORD"),
                environment.get("FLYWAY_VALIDATE_ON_MIGRATE"),
                phase2a ? "jaldishop_fase2a_test" : "jaldishop_integration_test", urlVariable);
    }

    static TestPostgresConfig fromValues(String url, String username, String password) {
        return fromValues(url, username, password, null);
    }

    static TestPostgresConfig fromValues(String url, String username, String password, String flywayValidateOnMigrate) {
        return fromValuesFor(url, username, password, flywayValidateOnMigrate, "jaldishop_integration_test", "DB_URL");
    }

    static TestPostgresConfig fromValuesForPhase2a(String url, String username, String password) {
        return fromValuesFor(url, username, password, null, "jaldishop_fase2a_test", "PHASE2A_DB_URL");
    }

    private static TestPostgresConfig fromValuesFor(String url, String username, String password,
                                                    String flywayValidateOnMigrate, String expectedDatabase, String urlVariable) {
        if (url == null || username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException(urlVariable + ", DB_USERNAME and DB_PASSWORD must be set in the process environment for integration tests.");
        }

        Matcher match = TEST_URL.matcher(url);
        if (!match.matches() || Integer.parseInt(match.group(2)) > 65535 || !expectedDatabase.equals(match.group(3))) {
            throw new IllegalStateException("Integration tests require a loopback PostgreSQL URL with an explicit port and database " + expectedDatabase + ", without parameters.");
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

    public static void requireDestructiveIntegrationDatabase(String actualDatabase) {
        if (!"jaldishop_integration_test".equals(actualDatabase))
            throw new IllegalStateException("FAIL-CLOSED: destructive fixtures require jaldishop_integration_test");
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

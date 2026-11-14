package com.jaldishop.backend.identity.infrastructure.mail;

import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;

class ConfiguredPasswordResetMailTest {
    @TempDir Path temporary;

    @Test void disabledDoesNotWriteMailOrRequireCredentials() throws Exception {
        var env = new MockEnvironment().withProperty("app.password-reset.mail.local-directory", temporary.toString());
        new ConfiguredPasswordResetMail(env).send("a@example.com", "sensitive-test-token", Instant.now());
        try (var files = Files.list(temporary)) { assertThat(files.count()).isZero(); }
    }

    @Test void localMailboxIsPrivateAndNeverUsesRecipientAsFilename() throws Exception {
        var env = new MockEnvironment().withProperty("app.password-reset.mail.mode", "local")
                .withProperty("app.password-reset.mail.local-directory", temporary.resolve("mail").toString());
        env.setActiveProfiles("local");
        new ConfiguredPasswordResetMail(env).send("a@example.com", "sensitive-test-token", Instant.now());
        Path dir = temporary.resolve("mail");
        assertThat(Files.getPosixFilePermissions(dir)).isEqualTo(PosixFilePermissions.fromString("rwx------"));
        try (var files = Files.list(dir)) {
            Path message = files.findFirst().orElseThrow();
            assertThat(message.getFileName().toString()).doesNotContain("example.com", "sensitive");
            assertThat(Files.getPosixFilePermissions(message)).isEqualTo(PosixFilePermissions.fromString("rw-------"));
            String content = Files.readString(message);
            assertThat(content).contains("To: a@example.com", "sensitive-test-token");
            assertThat(content).contains("http://localhost:3000/reset-password?token=sensitive-test-token");
            assertThat(content).doesNotContain("POST /api/v1/auth/reset-password");
        }
    }

    @Test void localMailboxUsesCustomResetUrlWhenConfigured() throws Exception {
        var env = new MockEnvironment().withProperty("app.password-reset.mail.mode", "local")
                .withProperty("app.password-reset.mail.local-directory", temporary.resolve("custom-mail").toString())
                .withProperty("app.password-reset.mail.reset-url-base", "https://www.jaldishop.net/reset-password");
        env.setActiveProfiles("local");
        new ConfiguredPasswordResetMail(env).send("cliente@example.test", "token-xyz-123", Instant.now());
        Path dir = temporary.resolve("custom-mail");
        try (var files = Files.list(dir)) {
            Path message = files.findFirst().orElseThrow();
            String content = Files.readString(message);
            assertThat(content).contains("https://www.jaldishop.net/reset-password?token=token-xyz-123");
        }
    }

    @Test void positiveValidProductionHttpsUrlAccepted() {
        var env = new MockEnvironment()
                .withProperty("app.password-reset.mail.mode", "resend")
                .withProperty("app.password-reset.mail.real-delivery-enabled", "true")
                .withProperty("app.password-reset.mail.from", "soporte@jaldishop.net")
                .withProperty("app.password-reset.mail.api-key", "re_test_key_123")
                .withProperty("app.password-reset.mail.reset-url-base", "https://www.jaldishop.net/reset-password");

        assertThatCode(() -> new ConfiguredPasswordResetMail(env)).doesNotThrowAnyException();
    }

    @Test void authorizedPublicDomainAccepted() {
        // Default authorized domains (jaldishop.pe, jaldishop.net, etc.)
        for (String validUrl : java.util.List.of(
                "https://jaldishop.pe/reset-password",
                "https://www.jaldishop.pe/reset-password",
                "https://jaldishop.net/reset-password",
                "https://www.jaldishop.net/reset-password",
                "https://jaldishop.com/reset-password"
        )) {
            var env = new MockEnvironment()
                    .withProperty("app.password-reset.mail.mode", "resend")
                    .withProperty("app.password-reset.mail.real-delivery-enabled", "true")
                    .withProperty("app.password-reset.mail.from", "soporte@jaldishop.net")
                    .withProperty("app.password-reset.mail.api-key", "re_test_key_123")
                    .withProperty("app.password-reset.mail.reset-url-base", validUrl);
            assertThatCode(() -> new ConfiguredPasswordResetMail(env)).doesNotThrowAnyException();
        }

        // Custom authorized host configuration
        var customEnv = new MockEnvironment()
                .withProperty("app.password-reset.mail.mode", "resend")
                .withProperty("app.password-reset.mail.real-delivery-enabled", "true")
                .withProperty("app.password-reset.mail.from", "soporte@jaldishop.net")
                .withProperty("app.password-reset.mail.api-key", "re_test_key_123")
                .withProperty("app.password-reset.mail.allowed-hosts", "marketplace.custom.org")
                .withProperty("app.password-reset.mail.reset-url-base", "https://marketplace.custom.org/reset-password");
        assertThatCode(() -> new ConfiguredPasswordResetMail(customEnv)).doesNotThrowAnyException();
    }

    @Test void ipv4PrivateAndLoopbackRejected() {
        java.util.List<String> privateIpv4Urls = java.util.List.of(
                "https://127.0.0.1/reset-password",
                "https://127.0.0.2/reset-password",
                "https://127.255.255.255/reset-password",
                "https://10.0.0.1/reset-password",
                "https://10.255.255.255/reset-password",
                "https://172.16.0.1/reset-password",
                "https://172.31.255.255/reset-password",
                "https://192.168.0.1/reset-password",
                "https://192.168.1.100/reset-password",
                "https://192.168.255.255/reset-password",
                "https://169.254.1.1/reset-password",
                "https://0.0.0.0/reset-password"
        );

        for (String url : privateIpv4Urls) {
            var env = new MockEnvironment()
                    .withProperty("app.password-reset.mail.mode", "resend")
                    .withProperty("app.password-reset.mail.real-delivery-enabled", "true")
                    .withProperty("app.password-reset.mail.from", "soporte@jaldishop.net")
                    .withProperty("app.password-reset.mail.api-key", "re_test_key_123")
                    .withProperty("app.password-reset.mail.reset-url-base", url);

            assertThatThrownBy(() -> new ConfiguredPasswordResetMail(env))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("La entrega real de correo no permite URL de recuperación en localhost, direcciones IP privadas o redes locales.");
        }
    }

    @Test void ipv6PrivateAndLoopbackRejected() {
        java.util.List<String> privateIpv6Urls = java.util.List.of(
                "https://[::1]/reset-password",
                "https://[fc00::1]/reset-password",
                "https://[fd12:3456:789a::1]/reset-password",
                "https://[fdff:ffff:ffff:ffff:ffff:ffff:ffff:ffff]/reset-password",
                "https://[fe80::1]/reset-password"
        );

        for (String url : privateIpv6Urls) {
            var env = new MockEnvironment()
                    .withProperty("app.password-reset.mail.mode", "resend")
                    .withProperty("app.password-reset.mail.real-delivery-enabled", "true")
                    .withProperty("app.password-reset.mail.from", "soporte@jaldishop.net")
                    .withProperty("app.password-reset.mail.api-key", "re_test_key_123")
                    .withProperty("app.password-reset.mail.reset-url-base", url);

            assertThatThrownBy(() -> new ConfiguredPasswordResetMail(env))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("La entrega real de correo no permite URL de recuperación en localhost, direcciones IP privadas o redes locales.");
        }
    }

    @Test void unauthorizedDomainRejectedWithoutLeakingSensitiveInput() {
        for (String unauthorizedUrl : java.util.List.of(
                "https://evil-phishing.com/reset-password",
                "https://attacker.org/reset-password",
                "https://jaldishop.pe.attacker.org/reset-password"
        )) {
            var env = new MockEnvironment()
                    .withProperty("app.password-reset.mail.mode", "resend")
                    .withProperty("app.password-reset.mail.real-delivery-enabled", "true")
                    .withProperty("app.password-reset.mail.from", "soporte@jaldishop.net")
                    .withProperty("app.password-reset.mail.api-key", "re_test_key_123")
                    .withProperty("app.password-reset.mail.reset-url-base", unauthorizedUrl);

            assertThatThrownBy(() -> new ConfiguredPasswordResetMail(env))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("El dominio de la URL de recuperación no está autorizado.")
                    .matches(ex -> !ex.getMessage().contains("evil-phishing.com")
                            && !ex.getMessage().contains("attacker.org"), "No debe reflejar dominios no autorizados");
        }
    }

    @Test void httpProtocolRejectedInRealDeliveryMode() {
        for (String httpUrl : java.util.List.of(
                "http://jaldishop.pe/reset-password",
                "http://www.jaldishop.net/reset-password"
        )) {
            var env = new MockEnvironment()
                    .withProperty("app.password-reset.mail.mode", "resend")
                    .withProperty("app.password-reset.mail.real-delivery-enabled", "true")
                    .withProperty("app.password-reset.mail.from", "soporte@jaldishop.net")
                    .withProperty("app.password-reset.mail.api-key", "re_test_key_123")
                    .withProperty("app.password-reset.mail.reset-url-base", httpUrl);

            assertThatThrownBy(() -> new ConfiguredPasswordResetMail(env))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("La entrega real de correo requiere HTTPS en la URL de recuperación.");
        }
    }

    @Test void localConfigurationPermittedInNonRealDeliveryMode() {
        // Default fallback in disabled mode
        var envDisabledDefault = new MockEnvironment()
                .withProperty("app.password-reset.mail.mode", "disabled");
        assertThatCode(() -> new ConfiguredPasswordResetMail(envDisabledDefault)).doesNotThrowAnyException();

        // Local development in local mode
        var envLocalDev = new MockEnvironment()
                .withProperty("app.password-reset.mail.mode", "local")
                .withProperty("app.password-reset.mail.reset-url-base", "http://localhost:3000/reset-password");
        envLocalDev.setActiveProfiles("local");
        assertThatCode(() -> new ConfiguredPasswordResetMail(envLocalDev)).doesNotThrowAnyException();

        // Loopback IP in disabled mode
        var envLoopback = new MockEnvironment()
                .withProperty("app.password-reset.mail.mode", "disabled")
                .withProperty("app.password-reset.mail.reset-url-base", "http://127.0.0.1:3000/reset-password");
        assertThatCode(() -> new ConfiguredPasswordResetMail(envLoopback)).doesNotThrowAnyException();
    }

    @Test void providerPayloadRedactsDebugLogsButSerializesTheMail() throws Exception {
        var payload = new ConfiguredPasswordResetMail.MailPayload("from@example.test",
                java.util.List.of("to@example.test"), "Reset", "private-token-in-mail");
        assertThat(payload.toString()).doesNotContain("private-token-in-mail", "to@example.test");
        String json = new tools.jackson.databind.ObjectMapper().writeValueAsString(payload);
        assertThat(json).contains("\"text\":\"private-token-in-mail\"", "\"to\":[\"to@example.test\"]");
    }

    @Test void realMailRequiresOptInAndLocalModeRequiresLocalProfile() {
        assertThatThrownBy(() -> new ConfiguredPasswordResetMail(new MockEnvironment()
                .withProperty("app.password-reset.mail.mode", "resend"))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ConfiguredPasswordResetMail(new MockEnvironment()
                .withProperty("app.password-reset.mail.mode", "resend")
                .withProperty("app.password-reset.mail.real-delivery-enabled", "true")))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ConfiguredPasswordResetMail(new MockEnvironment()
                .withProperty("app.password-reset.mail.mode", "local"))).isInstanceOf(IllegalStateException.class);
    }

    @Test void realMailRejectsMissingUrlBaseOrLocalhostFallback() {
        var envNoUrl = new MockEnvironment()
                .withProperty("app.password-reset.mail.mode", "resend")
                .withProperty("app.password-reset.mail.real-delivery-enabled", "true")
                .withProperty("app.password-reset.mail.from", "soporte@jaldishop.net")
                .withProperty("app.password-reset.mail.api-key", "re_test_key");
        assertThatThrownBy(() -> new ConfiguredPasswordResetMail(envNoUrl))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Falta configuración de URL de recuperación");

        var envLocalhost = new MockEnvironment()
                .withProperty("app.password-reset.mail.mode", "resend")
                .withProperty("app.password-reset.mail.real-delivery-enabled", "true")
                .withProperty("app.password-reset.mail.from", "soporte@jaldishop.net")
                .withProperty("app.password-reset.mail.api-key", "re_test_key")
                .withProperty("app.password-reset.mail.reset-url-base", "http://localhost:3000/reset-password");
        assertThatThrownBy(() -> new ConfiguredPasswordResetMail(envLocalhost))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requiere HTTPS");

        var envHttpsLocalhost = new MockEnvironment()
                .withProperty("app.password-reset.mail.mode", "resend")
                .withProperty("app.password-reset.mail.real-delivery-enabled", "true")
                .withProperty("app.password-reset.mail.from", "soporte@jaldishop.net")
                .withProperty("app.password-reset.mail.api-key", "re_test_key")
                .withProperty("app.password-reset.mail.reset-url-base", "https://localhost/reset-password");
        assertThatThrownBy(() -> new ConfiguredPasswordResetMail(envHttpsLocalhost))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no permite URL de recuperación en localhost, direcciones IP privadas o redes locales");

        var envHttpsIpLoopback = new MockEnvironment()
                .withProperty("app.password-reset.mail.mode", "resend")
                .withProperty("app.password-reset.mail.real-delivery-enabled", "true")
                .withProperty("app.password-reset.mail.from", "soporte@jaldishop.net")
                .withProperty("app.password-reset.mail.api-key", "re_test_key")
                .withProperty("app.password-reset.mail.reset-url-base", "https://127.0.0.1/reset-password");
        assertThatThrownBy(() -> new ConfiguredPasswordResetMail(envHttpsIpLoopback))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no permite URL de recuperación en localhost, direcciones IP privadas o redes locales");

        var envHttpsLocalDomain = new MockEnvironment()
                .withProperty("app.password-reset.mail.mode", "resend")
                .withProperty("app.password-reset.mail.real-delivery-enabled", "true")
                .withProperty("app.password-reset.mail.from", "soporte@jaldishop.net")
                .withProperty("app.password-reset.mail.api-key", "re_test_key")
                .withProperty("app.password-reset.mail.reset-url-base", "https://jaldishop.internal/reset-password");
        assertThatThrownBy(() -> new ConfiguredPasswordResetMail(envHttpsLocalDomain))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no permite URL de recuperación en localhost, direcciones IP privadas o redes locales");

        var envHttpProduction = new MockEnvironment()
                .withProperty("app.password-reset.mail.mode", "resend")
                .withProperty("app.password-reset.mail.real-delivery-enabled", "true")
                .withProperty("app.password-reset.mail.from", "soporte@jaldishop.net")
                .withProperty("app.password-reset.mail.api-key", "re_test_key")
                .withProperty("app.password-reset.mail.reset-url-base", "http://jaldishop.net/reset-password");
        assertThatThrownBy(() -> new ConfiguredPasswordResetMail(envHttpProduction))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requiere HTTPS");
    }

    @Test void strictUrlValidationRejectsUserInfoQueryFragmentAndWrongPath() {
        var baseEnv = new MockEnvironment()
                .withProperty("app.password-reset.mail.mode", "resend")
                .withProperty("app.password-reset.mail.real-delivery-enabled", "true")
                .withProperty("app.password-reset.mail.from", "soporte@jaldishop.net")
                .withProperty("app.password-reset.mail.api-key", "re_test_key");

        // User info rejected
        var envUserInfo = new MockEnvironment()
                .withProperty("app.password-reset.mail.mode", "resend")
                .withProperty("app.password-reset.mail.real-delivery-enabled", "true")
                .withProperty("app.password-reset.mail.from", "soporte@jaldishop.net")
                .withProperty("app.password-reset.mail.api-key", "re_test_key")
                .withProperty("app.password-reset.mail.reset-url-base", "https://admin:secret@jaldishop.net/reset-password");
        assertThatThrownBy(() -> new ConfiguredPasswordResetMail(envUserInfo))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no debe contener credenciales de usuario");

        // Pre-existing query rejected
        var envQuery = new MockEnvironment()
                .withProperty("app.password-reset.mail.mode", "resend")
                .withProperty("app.password-reset.mail.real-delivery-enabled", "true")
                .withProperty("app.password-reset.mail.from", "soporte@jaldishop.net")
                .withProperty("app.password-reset.mail.api-key", "re_test_key")
                .withProperty("app.password-reset.mail.reset-url-base", "https://www.jaldishop.net/reset-password?preexisting=param");
        assertThatThrownBy(() -> new ConfiguredPasswordResetMail(envQuery))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no debe contener parámetros de consulta preexistentes");

        // Fragment rejected
        var envFragment = new MockEnvironment()
                .withProperty("app.password-reset.mail.mode", "resend")
                .withProperty("app.password-reset.mail.real-delivery-enabled", "true")
                .withProperty("app.password-reset.mail.from", "soporte@jaldishop.net")
                .withProperty("app.password-reset.mail.api-key", "re_test_key")
                .withProperty("app.password-reset.mail.reset-url-base", "https://www.jaldishop.net/reset-password#anchor");
        assertThatThrownBy(() -> new ConfiguredPasswordResetMail(envFragment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no debe contener fragmentos");

        // Wrong path rejected
        var envWrongPath = new MockEnvironment()
                .withProperty("app.password-reset.mail.mode", "resend")
                .withProperty("app.password-reset.mail.real-delivery-enabled", "true")
                .withProperty("app.password-reset.mail.from", "soporte@jaldishop.net")
                .withProperty("app.password-reset.mail.api-key", "re_test_key")
                .withProperty("app.password-reset.mail.reset-url-base", "https://www.jaldishop.net/other-target");
        assertThatThrownBy(() -> new ConfiguredPasswordResetMail(envWrongPath))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("debe corresponder exactamente a /reset-password");

        // Relative URL rejected
        var envRelative = new MockEnvironment()
                .withProperty("app.password-reset.mail.mode", "local")
                .withProperty("app.password-reset.mail.reset-url-base", "/reset-password");
        envRelative.setActiveProfiles("local");
        assertThatThrownBy(() -> new ConfiguredPasswordResetMail(envRelative))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("debe ser absoluta");
    }

    @Test void malformedOrInjectedUrlsAreRejectedWithoutEchoingInput() {
        var envNewline = new MockEnvironment()
                .withProperty("app.password-reset.mail.mode", "local")
                .withProperty("app.password-reset.mail.reset-url-base", "http://localhost:3000/reset\nInjected: true");
        envNewline.setActiveProfiles("local");
        assertThatThrownBy(() -> new ConfiguredPasswordResetMail(envNewline))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("URL de recuperación inválida.");

        var envNoScheme = new MockEnvironment()
                .withProperty("app.password-reset.mail.mode", "local")
                .withProperty("app.password-reset.mail.reset-url-base", "jaldishop.net/reset-password");
        envNoScheme.setActiveProfiles("local");
        assertThatThrownBy(() -> new ConfiguredPasswordResetMail(envNoScheme))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("La URL de recuperación debe ser absoluta.");

        var envBadSyntax = new MockEnvironment()
                .withProperty("app.password-reset.mail.mode", "local")
                .withProperty("app.password-reset.mail.reset-url-base", "http://[invalid-ipv6-host/reset-password");
        envBadSyntax.setActiveProfiles("local");
        assertThatThrownBy(() -> new ConfiguredPasswordResetMail(envBadSyntax))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("URL de recuperación con sintaxis inválida.");
    }
}

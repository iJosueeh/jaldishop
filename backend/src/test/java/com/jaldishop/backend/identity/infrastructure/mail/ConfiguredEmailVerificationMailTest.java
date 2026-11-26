package com.jaldishop.backend.identity.infrastructure.mail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfiguredEmailVerificationMailTest {

    @TempDir
    Path tempDir;

    @Test
    void writesHtmlAndPlainTextFallbackToLocalMailboxWithoutLeakingInToString() throws Exception {
        MockEnvironment env = new MockEnvironment()
                .withProperty("app.password-reset.mail.mode", "local")
                .withProperty("app.email-verification.mail.local-directory", tempDir.toString())
                .withProperty("app.email-verification.mail.verify-url-base", "http://localhost:3000/verify-email");
        env.setActiveProfiles("test");

        ConfiguredEmailVerificationMail mail = new ConfiguredEmailVerificationMail(env);
        mail.sendVerification("ana@example.com", "Ana <script>", "abc123token", Instant.parse("2026-05-02T12:00:00Z"));

        try (var files = Files.list(tempDir)) {
            List<Path> created = files.toList();
            assertThat(created).hasSize(1);
            String content = Files.readString(created.getFirst());
            assertThat(content).contains("http://localhost:3000/verify-email?token=abc123token");
            assertThat(content).contains("Ana &lt;script&gt;");
            assertThat(content).contains("Verificar mi correo");
            assertThat(content).contains("PLAIN TEXT FALLBACK");
        }

        var payload = new ConfiguredEmailVerificationMail.MailPayload(
                "no-reply@jaldishop.com",
                List.of("ana@example.com"),
                "Verifica tu correo",
                "<p>secret</p>",
                "secret"
        );
        assertThat(payload.toString()).isEqualTo("EmailVerificationMail[REDACTED]");
    }

    @Test
    void rejectsNonHttpsOrUnauthorizedHostsInResendMode() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("app.password-reset.mail.mode", "resend")
                .withProperty("app.password-reset.mail.real-delivery-enabled", "true")
                .withProperty("app.password-reset.mail.api-key", "re_test_123")
                .withProperty("app.password-reset.mail.from", "no-reply@jaldishop.com")
                .withProperty("app.password-reset.mail.allowed-hosts", "jaldishop.com")
                .withProperty("app.email-verification.mail.verify-url-base", "http://jaldishop.com/verify-email");

        assertThatThrownBy(() -> new ConfiguredEmailVerificationMail(env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTPS");
    }
}

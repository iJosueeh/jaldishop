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
            assertThat(Files.readString(message)).contains("To: a@example.com", "sensitive-test-token");
        }
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
}

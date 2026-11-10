package com.jaldishop.backend.identity.infrastructure.mail;

import com.jaldishop.backend.identity.application.port.PasswordResetMail;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;

@Component
public class ConfiguredPasswordResetMail implements PasswordResetMail {
    private final String mode;
    private final Path mailbox;
    private final RestClient provider;
    private final String from;
    public ConfiguredPasswordResetMail(Environment env) {
        mode = env.getProperty("app.password-reset.mail.mode", "disabled");
        mailbox = Path.of(env.getProperty("app.password-reset.mail.local-directory", ".local/password-reset-mail"));
        from = env.getProperty("app.password-reset.mail.from", "");

        if (!java.util.Set.of("disabled", "local", "resend").contains(mode))
            throw new IllegalStateException("Modo de correo de recuperación inválido.");
        if (mode.equals("local") && !env.matchesProfiles("local", "test"))
            throw new IllegalStateException("El buzón de desarrollo requiere el perfil local o test.");
        if (mode.equals("resend")) {
            if (!env.getProperty("app.password-reset.mail.real-delivery-enabled", Boolean.class, false))
                throw new IllegalStateException("La entrega real de correo requiere habilitación explícita.");
            String apiKey = required(env, "app.password-reset.mail.api-key");
            if (from.isBlank() || from.contains("\r") || from.contains("\n"))
                throw new IllegalStateException("Remitente de recuperación requerido.");
            var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5)).build());
            factory.setReadTimeout(Duration.ofSeconds(5));
            provider = RestClient.builder().baseUrl("https://api.resend.com")
                    .requestFactory(factory).defaultHeader("Authorization", "Bearer " + apiKey).build();
        } else {
            provider = null;
        }
    }
    // RestClient DEBUG logs call toString() on the outgoing body.
    record MailPayload(String from, List<String> to, String subject, String text) {
        @Override public String toString() { return "PasswordResetMail[REDACTED]"; }
    }

    private static String required(Environment env, String key) {
        String value = env.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalStateException("Falta configuración de correo: " + key);
        return value;
    }
    public void send(String recipient, String token, Instant expiresAt) {
        if (mode.equals("disabled")) return;
        if (recipient.contains("\r") || recipient.contains("\n")) throw new IllegalArgumentException("Destinatario inválido.");
        String body = "Restablece tu contraseña con POST /api/v1/auth/reset-password.\n"
                + "Token: " + token + "\nVence: " + expiresAt + "\nSi no solicitaste el cambio, ignora este correo.\n";
        if (mode.equals("local")) {
            try {
                Files.createDirectories(mailbox, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
                Files.setPosixFilePermissions(mailbox, PosixFilePermissions.fromString("rwx------"));
                Path file = Files.createTempFile(mailbox, "message-", ".eml",
                        PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
                Files.writeString(file, "To: " + recipient + "\nSubject: Restablecer contraseña JaldiShop\n\n" + body);
            } catch (IOException failure) { throw new IllegalStateException("No se pudo guardar el correo local."); }
        } else {
            // Discard provider error bodies: they may echo the message containing the token.
            try {
                provider.post().uri("/emails").contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .body(new MailPayload(from, List.of(recipient), "Restablecer contraseña JaldiShop", body))
                        .exchange((request, response) -> {
                            if (!response.getStatusCode().is2xxSuccessful())
                                throw new IllegalStateException("El proveedor no aceptó el correo de recuperación.");
                            return null;
                        });
            } catch (RuntimeException failure) {
                throw new IllegalStateException("No se pudo enviar el correo de recuperación.");
            }
        }
    }
}

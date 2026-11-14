package com.jaldishop.backend.identity.infrastructure.mail;

import com.jaldishop.backend.identity.application.port.PasswordResetMail;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.net.URI;
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
    private final String resetUrlBase;

    public ConfiguredPasswordResetMail(Environment env) {
        mode = env.getProperty("app.password-reset.mail.mode", "disabled");
        mailbox = Path.of(env.getProperty("app.password-reset.mail.local-directory", ".local/password-reset-mail"));
        from = env.getProperty("app.password-reset.mail.from", "");

        if (!java.util.Set.of("disabled", "local", "resend").contains(mode))
            throw new IllegalStateException("Modo de correo de recuperación inválido.");
        if (mode.equals("local") && !env.matchesProfiles("local", "test"))
            throw new IllegalStateException("El buzón de desarrollo requiere el perfil local o test.");

        String rawUrlBase = env.getProperty("app.password-reset.mail.reset-url-base", "");
        if (mode.equals("resend")) {
            if (!env.getProperty("app.password-reset.mail.real-delivery-enabled", Boolean.class, false))
                throw new IllegalStateException("La entrega real de correo requiere habilitación explícita.");
            String apiKey = required(env, "app.password-reset.mail.api-key");
            if (from.isBlank() || from.contains("\r") || from.contains("\n"))
                throw new IllegalStateException("Remitente de recuperación requerido.");
            if (rawUrlBase.isBlank())
                throw new IllegalStateException("Falta configuración de URL de recuperación: app.password-reset.mail.reset-url-base");
            this.resetUrlBase = validateResetUrl(rawUrlBase, true);
            var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5)).build());
            factory.setReadTimeout(Duration.ofSeconds(5));
            provider = RestClient.builder().baseUrl("https://api.resend.com")
                    .requestFactory(factory).defaultHeader("Authorization", "Bearer " + apiKey).build();
        } else {
            this.resetUrlBase = rawUrlBase.isBlank() ? "http://localhost:3000/reset-password" : validateResetUrl(rawUrlBase, false);
            provider = null;
        }
    }

    private static String validateResetUrl(String rawUrl, boolean requireHttps) {
        if (rawUrl.contains("\r") || rawUrl.contains("\n") || rawUrl.contains(" "))
            throw new IllegalStateException("URL de recuperación inválida.");
        URI uri;
        try {
            uri = URI.create(rawUrl);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("URL de recuperación con sintaxis inválida: " + rawUrl);
        }
        String scheme = uri.getScheme();
        if (scheme == null)
            throw new IllegalStateException("URL de recuperación sin protocolo.");
        if (requireHttps) {
            if (!"https".equalsIgnoreCase(scheme))
                throw new IllegalStateException("La entrega real de correo requiere HTTPS en la URL de recuperación.");
            String host = uri.getHost();
            if (host == null || host.isBlank())
                throw new IllegalStateException("URL de recuperación sin host válido.");
            String lowerHost = host.toLowerCase();
            if (lowerHost.equals("localhost") || lowerHost.equals("127.0.0.1") || lowerHost.endsWith(".local") || lowerHost.endsWith(".localhost"))
                throw new IllegalStateException("La entrega real de correo no permite URL de recuperación en localhost.");
        } else {
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
                throw new IllegalStateException("Protocolo de URL de recuperación no admitido.");
        }
        return rawUrl;
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
        String resetLink = resetUrlBase + (resetUrlBase.contains("?") ? "&token=" : "?token=") + token;
        String body = "Hola,\n\n"
                + "Recibimos una solicitud para restablecer la contraseña de tu cuenta en JaldiShop.\n\n"
                + "Para crear una nueva contraseña, ingresa al siguiente enlace:\n"
                + resetLink + "\n\n"
                + "Este enlace es de uso único y vencerá en 15 minutos (a las " + expiresAt + ").\n\n"
                + "Si tú no solicitaste este cambio, puedes ignorar este mensaje de forma segura; tu contraseña actual no cambiará.\n";
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

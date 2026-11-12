package com.jaldishop.backend.identity.infrastructure.mail;

import com.jaldishop.backend.identity.application.port.AdminInvitationMail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ConfiguredAdminInvitationMail implements AdminInvitationMail {

    private static final Logger log = LoggerFactory.getLogger(ConfiguredAdminInvitationMail.class);

    private final String mode;
    private final Path mailbox;
    private final RestClient provider;
    private final String from;

    public ConfiguredAdminInvitationMail(Environment env) {
        this.mode = env.getProperty("app.password-reset.mail.mode", "disabled");
        this.mailbox = Path.of(env.getProperty("app.admin-invitation.mail.local-directory", ".local/admin-invitation-mail"));
        this.from = env.getProperty("app.password-reset.mail.from", "soporte@jaldishop.com");

        if (mode.equals("resend") && env.getProperty("app.password-reset.mail.real-delivery-enabled", Boolean.class, false)) {
            String apiKey = env.getProperty("app.password-reset.mail.api-key", "");
            var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5)).build());
            factory.setReadTimeout(Duration.ofSeconds(5));
            this.provider = RestClient.builder().baseUrl("https://api.resend.com")
                    .requestFactory(factory).defaultHeader("Authorization", "Bearer " + apiKey).build();
        } else {
            this.provider = null;
        }
    }

    record MailPayload(String from, List<String> to, String subject, String text) {
        @Override
        public String toString() { return "AdminInvitationMail[REDACTED]"; }
    }

    @Override
    public void sendInvitation(String recipientEmail, String recipientName, String rawToken, Instant expiresAt) {
        if (mode.equals("disabled")) {
            return;
        }
        if (recipientEmail.contains("\r") || recipientEmail.contains("\n")) {
            throw new IllegalArgumentException("Destinatario inválido.");
        }

        String body = "Hola " + recipientName + ",\n\n"
                + "Has sido invitado como Administrador en JaldiShop.\n"
                + "Completa tu registro con POST /api/v1/auth/admin-invitations/accept.\n"
                + "Token de invitación: " + rawToken + "\n"
                + "Vence: " + expiresAt + "\n";

        if (mode.equals("local")) {
            try {
                Files.createDirectories(mailbox);
                Path mailFile = mailbox.resolve(UUID.randomUUID() + ".txt");
                Files.writeString(mailFile, "To: " + recipientEmail + "\n" + body,
                        StandardOpenOption.CREATE_NEW);
            } catch (IOException e) {
                log.error("Fallo al escribir correo local de invitación: {}", e.getMessage());
            }
            return;
        }

        if (provider != null) {
            try {
                provider.post().uri("/emails")
                        .body(new MailPayload(from, List.of(recipientEmail), "Invitación Administrativa - JaldiShop", body))
                        .retrieve().toBodilessEntity();
            } catch (Exception ex) {
                log.error("Fallo al despachar correo de invitación vía Resend: {}", ex.getMessage());
            }
        }
    }
}

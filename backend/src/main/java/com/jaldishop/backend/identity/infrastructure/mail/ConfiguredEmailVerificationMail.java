package com.jaldishop.backend.identity.infrastructure.mail;

import com.jaldishop.backend.identity.application.port.EmailVerificationMail;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Component
public class ConfiguredEmailVerificationMail implements EmailVerificationMail {

    private final String mode;
    private final Path mailbox;
    private final RestClient provider;
    private final String from;
    private final URI verifyBaseUri;

    public ConfiguredEmailVerificationMail(Environment env) {
        this.mode = env.getProperty("app.password-reset.mail.mode", "disabled");
        this.mailbox = Path.of(env.getProperty("app.email-verification.mail.local-directory", ".local/email-verification-mail"));
        this.from = env.getProperty("app.password-reset.mail.from", "");

        MailDeliverySupport.validateMailMode(mode, env, "Modo de correo de verificación inválido.");

        String rawUrlBase = env.getProperty("app.email-verification.mail.verify-url-base", "");
        Set<String> allowedHosts = MailDeliverySupport.parseAllowedHosts(
                env.getProperty("app.password-reset.mail.allowed-hosts", "")
        );

        if ("resend".equals(mode)) {
            if (!env.getProperty("app.password-reset.mail.real-delivery-enabled", Boolean.class, false)) {
                throw new IllegalStateException("La entrega real de correo requiere habilitación explícita.");
            }
            String apiKey = MailDeliverySupport.required(env, "app.password-reset.mail.api-key");
            if (from.isBlank() || from.contains("\r") || from.contains("\n")) {
                throw new IllegalStateException("Remitente de verificación requerido.");
            }
            if (rawUrlBase.isBlank()) {
                throw new IllegalStateException("Falta configuración de URL de verificación: app.email-verification.mail.verify-url-base");
            }
            if (allowedHosts.isEmpty()) {
                throw new IllegalStateException("La entrega real de correo requiere una lista explícita de hosts autorizados.");
            }
            this.verifyBaseUri = MailDeliverySupport.validateCallbackUrl(
                    rawUrlBase, "/verify-email", true, allowedHosts, "verificación"
            );
            this.provider = MailDeliverySupport.createResendClient(apiKey);
        } else {
            this.verifyBaseUri = rawUrlBase.isBlank()
                    ? URI.create("http://localhost:3000/verify-email")
                    : MailDeliverySupport.validateCallbackUrl(
                            rawUrlBase, "/verify-email", false, allowedHosts, "verificación"
                    );
            this.provider = null;
        }
    }

    // RestClient DEBUG logs call toString() on the outgoing body.
    record MailPayload(String from, List<String> to, String subject, String html, String text) {
        @Override
        public String toString() {
            return "EmailVerificationMail[REDACTED]";
        }
    }

    @Override
    public void sendVerification(String recipientEmail, String recipientName, String token, Instant expiresAt) {
        if ("disabled".equals(mode)) {
            return;
        }
        if (recipientEmail == null || recipientEmail.contains("\r") || recipientEmail.contains("\n")) {
            throw new IllegalArgumentException("Destinatario inválido.");
        }

        URI verifyUri = UriComponentsBuilder.fromUri(verifyBaseUri)
                .replaceQueryParam("token", token)
                .build(true)
                .toUri();
        String verifyLink = verifyUri.toASCIIString();
        String safeName = (recipientName == null || recipientName.isBlank()) ? "cliente" : recipientName.trim();

        String plainText = buildPlainText(safeName, verifyLink, expiresAt);
        String htmlBody = buildHtml(safeName, verifyLink);

        if ("local".equals(mode)) {
            String rawMessage = "To: " + recipientEmail + "\n"
                    + "Subject: Verifica tu correo en JaldiShop\n"
                    + "Content-Type: text/html; charset=UTF-8\n\n"
                    + htmlBody + "\n\n<!-- PLAIN TEXT FALLBACK:\n" + plainText + "\n-->";
            MailDeliverySupport.writeLocalMail(mailbox, rawMessage);
            return;
        }

        MailDeliverySupport.sendViaResend(
                provider,
                new MailPayload(from, List.of(recipientEmail), "Verifica tu correo en JaldiShop", htmlBody, plainText),
                "El proveedor no aceptó el correo de verificación.",
                "No se pudo enviar el correo de verificación."
        );
    }

    private String buildPlainText(String name, String verifyLink, Instant expiresAt) {
        return "Hola " + name + ",\n\n"
                + "¡Bienvenido a JaldiShop! Para activar tu cuenta y comenzar a comprar en tus comercios locales favoritos, verifica tu correo electrónico ingresando al siguiente enlace:\n\n"
                + verifyLink + "\n\n"
                + "Este enlace es de uso único y vencerá en 24 horas (a las " + expiresAt + ").\n\n"
                + "Si tú no creaste esta cuenta en JaldiShop, puedes ignorar este mensaje de forma segura.\n";
    }

    private String buildHtml(String name, String verifyLink) {
        String escapedName = HtmlUtils.htmlEscape(name);
        String escapedLink = HtmlUtils.htmlEscape(verifyLink);
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Verifica tu correo en JaldiShop</title>
                </head>
                <body style="margin:0;padding:0;background-color:#faf7f2;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#1c1917;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color:#faf7f2;padding:32px 16px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:560px;background-color:#ffffff;border:1px solid #e7e0d6;border-radius:16px;overflow:hidden;">
                          <tr>
                            <td style="background-color:#005141;padding:28px 32px;text-align:left;">
                              <span style="display:inline-block;font-size:20px;font-weight:700;color:#ffffff;letter-spacing:-0.02em;">JaldiShop</span>
                              <span style="display:block;margin-top:4px;font-size:12px;color:#36f4a4;font-weight:600;text-transform:uppercase;letter-spacing:0.06em;">Verificación de cuenta</span>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px;">
                              <h1 style="margin:0 0 16px;font-size:22px;line-height:1.3;font-weight:700;color:#1c1917;">Confirma tu correo electrónico</h1>
                              <p style="margin:0 0 16px;font-size:15px;line-height:1.6;color:#57534e;">Hola <strong style="color:#1c1917;">%s</strong>,</p>
                              <p style="margin:0 0 24px;font-size:15px;line-height:1.6;color:#57534e;">Gracias por registrarte en JaldiShop. Para activar tu cuenta y comprar en comercios locales cerca de ti, confirma tu dirección de correo haciendo clic en el siguiente botón:</p>
                              <table role="presentation" cellspacing="0" cellpadding="0" style="margin:0 0 24px;">
                                <tr>
                                  <td align="center" style="background-color:#005141;border-radius:12px;">
                                    <a href="%s" target="_blank" rel="noopener noreferrer" style="display:inline-block;padding:14px 28px;font-size:15px;font-weight:700;color:#ffffff;text-decoration:none;border-radius:12px;">Verificar mi correo</a>
                                  </td>
                                </tr>
                              </table>
                              <p style="margin:0 0 8px;font-size:13px;line-height:1.5;color:#78716c;">Este enlace es de un solo uso y caduca en <strong>24 horas</strong>.</p>
                              <p style="margin:0 0 16px;font-size:13px;line-height:1.5;color:#78716c;">Si el botón no funciona, copia y pega este enlace en tu navegador:</p>
                              <p style="margin:0 0 24px;padding:12px;background-color:#faf7f2;border:1px solid #e7e0d6;border-radius:8px;font-size:12px;line-height:1.5;color:#005141;word-break:break-all;">%s</p>
                              <hr style="border:none;border-top:1px solid #e7e0d6;margin:24px 0;">
                              <p style="margin:0;font-size:12px;line-height:1.5;color:#78716c;">Si tú no creaste una cuenta en JaldiShop, puedes ignorar este mensaje de forma segura.</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(escapedName, escapedLink, escapedLink);
    }
}

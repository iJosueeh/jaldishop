package com.jaldishop.backend.identity.infrastructure.mail;

import com.jaldishop.backend.identity.application.port.PasswordResetMail;
import com.jaldishop.backend.identity.domain.enums.RoleName;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
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
    private final URI baseUri;
    private final URI merchantBaseUri;
    private final Set<String> allowedHosts;

    public ConfiguredPasswordResetMail(Environment env) {
        mode = env.getProperty("app.password-reset.mail.mode", "disabled");
        mailbox = Path.of(env.getProperty("app.password-reset.mail.local-directory", ".local/password-reset-mail"));
        from = env.getProperty("app.password-reset.mail.from", "");

        if (!java.util.Set.of("disabled", "local", "resend").contains(mode))
            throw new IllegalStateException("Modo de correo de recuperación inválido.");
        if (mode.equals("local") && !env.matchesProfiles("local", "test"))
            throw new IllegalStateException("El buzón de desarrollo requiere el perfil local o test.");

        String rawUrlBase = env.getProperty("app.password-reset.mail.reset-url-base", "");
        String rawMerchantUrlBase = env.getProperty("app.password-reset.mail.merchant-reset-url-base", "");
        String rawAllowedHosts = env.getProperty("app.password-reset.mail.allowed-hosts", "");
        Set<String> allowedHosts = parseAllowedHosts(rawAllowedHosts);
        this.allowedHosts = allowedHosts;

        if (mode.equals("resend")) {
            if (!env.getProperty("app.password-reset.mail.real-delivery-enabled", Boolean.class, false))
                throw new IllegalStateException("La entrega real de correo requiere habilitación explícita.");
            String apiKey = required(env, "app.password-reset.mail.api-key");
            if (from.isBlank() || from.contains("\r") || from.contains("\n"))
                throw new IllegalStateException("Remitente de recuperación requerido.");
            if (rawUrlBase.isBlank())
                throw new IllegalStateException("Falta configuración de URL de recuperación: app.password-reset.mail.reset-url-base");
            if (allowedHosts.isEmpty())
                throw new IllegalStateException("La entrega real de correo requiere una lista explícita de hosts autorizados.");
            this.baseUri = validateResetUrl(rawUrlBase, true, allowedHosts);
            this.merchantBaseUri = rawMerchantUrlBase.isBlank()
                    ? this.baseUri
                    : validateResetUrl(rawMerchantUrlBase, true, allowedHosts);
            var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5)).build());
            factory.setReadTimeout(Duration.ofSeconds(5));
            provider = RestClient.builder().baseUrl("https://api.resend.com")
                    .requestFactory(factory).defaultHeader("Authorization", "Bearer " + apiKey).build();
        } else {
            this.baseUri = rawUrlBase.isBlank()
                    ? URI.create("http://localhost:3000/reset-password")
                    : validateResetUrl(rawUrlBase, false, allowedHosts);
            this.merchantBaseUri = rawMerchantUrlBase.isBlank()
                    ? URI.create("http://localhost:4200/reset-password")
                    : validateResetUrl(rawMerchantUrlBase, false, allowedHosts);
            provider = null;
        }
    }

    private static URI validateResetUrl(String rawUrl, boolean requireHttps, Set<String> allowedHosts) {
        if (rawUrl.contains("\r") || rawUrl.contains("\n") || rawUrl.contains(" "))
            throw new IllegalStateException("URL de recuperación inválida.");
        URI uri;
        try {
            uri = URI.create(rawUrl);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("URL de recuperación con sintaxis inválida.");
        }

        if (!uri.isAbsolute())
            throw new IllegalStateException("La URL de recuperación debe ser absoluta.");

        if (uri.getUserInfo() != null)
            throw new IllegalStateException("La URL de recuperación no debe contener credenciales de usuario.");

        if (uri.getRawQuery() != null)
            throw new IllegalStateException("La URL de recuperación no debe contener parámetros de consulta preexistentes.");

        if (uri.getRawFragment() != null)
            throw new IllegalStateException("La URL de recuperación no debe contener fragmentos.");

        String scheme = uri.getScheme();
        if (scheme == null)
            throw new IllegalStateException("URL de recuperación sin protocolo.");

        String path = uri.getPath();
        if (path == null || !"/reset-password".equals(path))
            throw new IllegalStateException("La ruta de la URL de recuperación debe corresponder exactamente a /reset-password.");

        String host = uri.getHost();
        if (host == null || host.isBlank())
            throw new IllegalStateException("URL de recuperación sin host válido.");

        if (requireHttps) {
            if (!"https".equalsIgnoreCase(scheme))
                throw new IllegalStateException("La entrega real de correo requiere HTTPS en la URL de recuperación.");
            String lowerHost = host.toLowerCase();
            if (isLocalHost(lowerHost))
                throw new IllegalStateException("La entrega real de correo no permite URL de recuperación en localhost, direcciones IP privadas o redes locales.");
            if (!isHostAuthorized(lowerHost, allowedHosts))
                throw new IllegalStateException("El dominio de la URL de recuperación no está autorizado.");
        } else {
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
                throw new IllegalStateException("Protocolo de URL de recuperación no admitido.");
        }
        return uri;
    }

    private static Set<String> parseAllowedHosts(String raw) {
        if (raw == null || raw.isBlank()) return Set.of();
        Set<String> hosts = new HashSet<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim().toLowerCase();
            if (trimmed.isEmpty()) continue;
            if (trimmed.contains("*") || trimmed.contains("?") || trimmed.contains("/") || trimmed.contains(":")
                    || trimmed.startsWith(".") || trimmed.endsWith(".")) {
                throw new IllegalStateException("La lista de hosts autorizados no admite comodines ni patrones permisivos.");
            }
            hosts.add(trimmed);
        }
        return Collections.unmodifiableSet(hosts);
    }

    private static boolean isHostAuthorized(String host, Set<String> allowedHosts) {
        if (allowedHosts == null || allowedHosts.isEmpty() || host == null) return false;
        return allowedHosts.contains(host);
    }

    private static boolean isLocalHost(String lowerHost) {
        if (lowerHost.equals("localhost")
                || lowerHost.endsWith(".local") || lowerHost.endsWith(".localhost") || lowerHost.endsWith(".internal")
                || lowerHost.endsWith(".lan") || lowerHost.endsWith(".test") || lowerHost.endsWith(".example")
                || lowerHost.endsWith(".invalid")) {
            return true;
        }
        if (isIpLiteral(lowerHost) && isPrivateOrLocalIp(lowerHost)) {
            return true;
        }
        return false;
    }

    private static boolean isIpLiteral(String host) {
        if (host == null || host.isEmpty()) return false;
        if (host.startsWith("[") && host.endsWith("]")) return true;
        if (host.contains(":")) return true;
        String[] parts = host.split("\\.", -1);
        if (parts.length == 4) {
            for (String p : parts) {
                if (p.isEmpty() || p.length() > 3) return false;
                for (int i = 0; i < p.length(); i++) {
                    if (!Character.isDigit(p.charAt(i))) return false;
                }
                int val = Integer.parseInt(p);
                if (val < 0 || val > 255) return false;
            }
            return true;
        }
        return false;
    }

    private static boolean isPrivateOrLocalIp(String host) {
        String clean = host;
        if (clean.startsWith("[") && clean.endsWith("]")) {
            clean = clean.substring(1, clean.length() - 1);
        }
        InetAddress addr;
        try {
            addr = InetAddress.getByName(clean);
        } catch (Exception e) {
            return false;
        }
        if (addr.isLoopbackAddress() || addr.isAnyLocalAddress() || addr.isLinkLocalAddress() || addr.isSiteLocalAddress()) {
            return true;
        }
        byte[] bytes = addr.getAddress();
        // IPv6 Unique Local Address (ULA) RFC 4193: fc00::/7 (0xfc or 0xfd in first byte)
        if (bytes.length == 16 && ((bytes[0] & 0xff) & 0xfe) == 0xfc) {
            return true;
        }
        // IPv4-mapped IPv6 address (::ffff:x.x.x.x)
        if (bytes.length == 16) {
            boolean isIPv4Mapped = true;
            for (int i = 0; i < 10; i++) {
                if (bytes[i] != 0) { isIPv4Mapped = false; break; }
            }
            if (isIPv4Mapped && bytes[10] == (byte) 0xff && bytes[11] == (byte) 0xff) {
                byte[] ipv4 = new byte[4];
                System.arraycopy(bytes, 12, ipv4, 0, 4);
                try {
                    InetAddress v4 = InetAddress.getByAddress(ipv4);
                    return v4.isLoopbackAddress() || v4.isAnyLocalAddress() || v4.isLinkLocalAddress() || v4.isSiteLocalAddress();
                } catch (UnknownHostException e) {
                    return false;
                }
            }
        }
        return false;
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

    URI resolveBaseUri(Set<RoleName> roles, String origin) {
        boolean hasMerchantOrAdmin = roles != null && (roles.contains(RoleName.MERCHANT) || roles.contains(RoleName.ADMIN));
        boolean hasCustomer = roles != null && roles.contains(RoleName.CUSTOMER);
        boolean isMultiRole = hasCustomer && hasMerchantOrAdmin;

        // 1. Merchant o Admin puro: siempre enlace Merchant
        if (hasMerchantOrAdmin && !hasCustomer) {
            return merchantBaseUri;
        }

        // 2. Customer puro: siempre enlace Customer
        if (hasCustomer && !hasMerchantOrAdmin) {
            return baseUri;
        }

        // 3. Multirrol (CUSTOMER + MERCHANT/ADMIN): se guía por Origin autorizado
        if (isMultiRole && origin != null && !origin.isBlank()) {
            URI originUri = tryParseOrigin(origin);
            if (originUri != null && isOriginAuthorized(originUri)) {
                if (isSameOrigin(originUri, baseUri)) {
                    return baseUri;
                }
                if (isSameOrigin(originUri, merchantBaseUri)) {
                    return merchantBaseUri;
                }
            }
        }

        // Fallback: rol comercial o admin -> portal merchant; si no -> customer
        return hasMerchantOrAdmin ? merchantBaseUri : baseUri;
    }

    private URI tryParseOrigin(String origin) {
        try {
            URI uri = URI.create(origin);
            if (uri.getHost() != null && !uri.getHost().isBlank()) {
                return uri;
            }
        } catch (IllegalArgumentException ignored) {}
        return null;
    }

    private boolean isOriginAuthorized(URI originUri) {
        String host = originUri.getHost();
        if (host == null) return false;
        String lowerHost = host.toLowerCase(Locale.ROOT);
        if ("resend".equals(mode)) {
            return isHostAuthorized(lowerHost, allowedHosts);
        }
        if (!allowedHosts.isEmpty()) {
            return isHostAuthorized(lowerHost, allowedHosts);
        }
        return isHostMatching(lowerHost, baseUri) || isHostMatching(lowerHost, merchantBaseUri);
    }

    private static boolean isHostMatching(String host, URI target) {
        return target != null && target.getHost() != null && target.getHost().equalsIgnoreCase(host);
    }

    private static boolean isSameOrigin(URI originUri, URI baseUri) {
        if (originUri == null || baseUri == null) return false;
        String host1 = originUri.getHost();
        String host2 = baseUri.getHost();
        if (host1 == null || host2 == null || !host1.equalsIgnoreCase(host2)) return false;
        int port1 = originUri.getPort() != -1 ? originUri.getPort() : ("https".equalsIgnoreCase(originUri.getScheme()) ? 443 : 80);
        int port2 = baseUri.getPort() != -1 ? baseUri.getPort() : ("https".equalsIgnoreCase(baseUri.getScheme()) ? 443 : 80);
        return port1 == port2;
    }

    @Override
    public void send(String recipient, String token, Instant expiresAt) {
        send(recipient, token, expiresAt, Set.of(), null);
    }

    @Override
    public void send(String recipient, String token, Instant expiresAt, Set<RoleName> roles, String origin) {
        if (mode.equals("disabled")) return;
        if (recipient.contains("\r") || recipient.contains("\n")) throw new IllegalArgumentException("Destinatario inválido.");

        URI targetBase = resolveBaseUri(roles, origin);
        URI resetUri = UriComponentsBuilder.fromUri(targetBase)
                .replaceQueryParam("token", token)
                .build(true)
                .toUri();
        String resetLink = resetUri.toASCIIString();

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

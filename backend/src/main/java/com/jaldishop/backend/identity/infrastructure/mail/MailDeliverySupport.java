package com.jaldishop.backend.identity.infrastructure.mail;

import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Shared SSRF-safe callback URL validation, local mailbox persistence, and Resend HTTP delivery.
 */
final class MailDeliverySupport {

    private MailDeliverySupport() {}

    static void validateMailMode(String mode, Environment env, String invalidModeMessage) {
        if (!Set.of("disabled", "local", "resend").contains(mode)) {
            throw new IllegalStateException(invalidModeMessage);
        }
        if ("local".equals(mode) && !env.matchesProfiles("local", "test")) {
            throw new IllegalStateException("El buzón de desarrollo requiere el perfil local o test.");
        }
    }

    static String required(Environment env, String key) {
        String value = env.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta configuración de correo: " + key);
        }
        return value;
    }

    static RestClient createResendClient(String apiKey) {
        var factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
        );
        factory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder()
                .baseUrl("https://api.resend.com")
                .requestFactory(factory)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    static void writeLocalMail(Path mailbox, String rawMessage) {
        try {
            Files.createDirectories(mailbox, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
            Files.setPosixFilePermissions(mailbox, PosixFilePermissions.fromString("rwx------"));
            Path file = Files.createTempFile(mailbox, "message-", ".eml",
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            Files.writeString(file, rawMessage);
        } catch (IOException failure) {
            throw new IllegalStateException("No se pudo guardar el correo local.");
        }
    }

    static void sendViaResend(RestClient provider, Object payload, String rejectedMessage, String failedMessage) {
        // Discard provider error bodies: they may echo the message containing the token.
        try {
            provider.post()
                    .uri("/emails")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .exchange((request, response) -> {
                        if (!response.getStatusCode().is2xxSuccessful()) {
                            throw new IllegalStateException(rejectedMessage);
                        }
                        return null;
                    });
        } catch (RuntimeException failure) {
            if (failure instanceof IllegalStateException ise && rejectedMessage.equals(ise.getMessage())) {
                throw ise;
            }
            throw new IllegalStateException(failedMessage);
        }
    }

    static URI validateCallbackUrl(String rawUrl, String expectedPath, boolean requireHttps, Set<String> allowedHosts, String label) {
        if (rawUrl.contains("\r") || rawUrl.contains("\n") || rawUrl.contains(" ")) {
            throw new IllegalStateException("URL de " + label + " inválida.");
        }
        URI uri;
        try {
            uri = URI.create(rawUrl);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("URL de " + label + " con sintaxis inválida.");
        }

        if (!uri.isAbsolute()) {
            throw new IllegalStateException("La URL de " + label + " debe ser absoluta.");
        }
        if (uri.getUserInfo() != null) {
            throw new IllegalStateException("La URL de " + label + " no debe contener credenciales de usuario.");
        }
        if (uri.getRawQuery() != null) {
            throw new IllegalStateException("La URL de " + label + " no debe contener parámetros de consulta preexistentes.");
        }
        if (uri.getRawFragment() != null) {
            throw new IllegalStateException("La URL de " + label + " no debe contener fragmentos.");
        }

        String scheme = uri.getScheme();
        if (scheme == null) {
            throw new IllegalStateException("URL de " + label + " sin protocolo.");
        }

        String path = uri.getPath();
        if (path == null || !expectedPath.equals(path)) {
            throw new IllegalStateException("La ruta de la URL de " + label + " debe corresponder exactamente a " + expectedPath + ".");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalStateException("URL de " + label + " sin host válido.");
        }

        if (requireHttps) {
            if (!"https".equalsIgnoreCase(scheme)) {
                throw new IllegalStateException("La entrega real de correo requiere HTTPS en la URL de " + label + ".");
            }
            String lowerHost = host.toLowerCase(Locale.ROOT);
            if (isLocalHost(lowerHost)) {
                throw new IllegalStateException("La entrega real de correo no permite URL de " + label + " en localhost, direcciones IP privadas o redes locales.");
            }
            if (!isHostAuthorized(lowerHost, allowedHosts)) {
                throw new IllegalStateException("El dominio de la URL de " + label + " no está autorizado.");
            }
        } else {
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new IllegalStateException("Protocolo de URL de " + label + " no admitido.");
            }
        }
        return uri;
    }

    static Set<String> parseAllowedHosts(String raw) {
        if (raw == null || raw.isBlank()) return Set.of();
        Set<String> hosts = new HashSet<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim().toLowerCase(Locale.ROOT);
            if (trimmed.isEmpty()) continue;
            if (trimmed.contains("*") || trimmed.contains("?") || trimmed.contains("/") || trimmed.contains(":")
                    || trimmed.startsWith(".") || trimmed.endsWith(".")) {
                throw new IllegalStateException("La lista de hosts autorizados no admite comodines ni patrones permisivos.");
            }
            hosts.add(trimmed);
        }
        return Collections.unmodifiableSet(hosts);
    }

    static boolean isHostAuthorized(String host, Set<String> allowedHosts) {
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
        return isIpLiteral(lowerHost) && isPrivateOrLocalIp(lowerHost);
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
        if (bytes.length == 16 && ((bytes[0] & 0xff) & 0xfe) == 0xfc) {
            return true;
        }
        if (bytes.length == 16) {
            boolean isIPv4Mapped = true;
            for (int i = 0; i < 10; i++) {
                if (bytes[i] != 0) {
                    isIPv4Mapped = false;
                    break;
                }
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
}

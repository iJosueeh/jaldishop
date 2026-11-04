package com.jaldishop.backend.payment.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Locale;

/**
 * Validador criptográfico de firmas para webhooks de Mercado Pago.
 * Implementa el protocolo oficial HMAC-SHA256 con verificación en tiempo constante
 * y tolerancia configurable a ataques de repetición (replay attacks).
 *
 * Manifest oficial: id:[data.id en minúsculas];request-id:[x-request-id];ts:[ts];
 */
@Component
public class MercadoPagoWebhookSignatureValidator {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookSignatureValidator.class);
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final long DEFAULT_MAX_TOLERANCE_SECONDS = 900; // 15 minutos

    public boolean isValid(String xSignature, String xRequestId, String dataId, String secretKey) {
        return isValid(xSignature, xRequestId, dataId, secretKey, DEFAULT_MAX_TOLERANCE_SECONDS, Instant.now().getEpochSecond());
    }

    public boolean isValid(String xSignature, String xRequestId, String dataId, String secretKey,
                           long maxToleranceSeconds, long currentEpochSeconds) {
        if (xSignature == null || xSignature.isBlank() ||
                xRequestId == null || xRequestId.isBlank() ||
                dataId == null || dataId.isBlank() ||
                secretKey == null || secretKey.isBlank()) {
            log.warn("Faltan parámetros obligatorios para verificar la firma del webhook de Mercado Pago.");
            return false;
        }

        String ts = null;
        String v1 = null;

        String[] parts = xSignature.split(",");
        for (String part : parts) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2) {
                String key = kv[0].trim();
                String value = kv[1].trim();
                if ("ts".equalsIgnoreCase(key)) {
                    ts = value;
                } else if ("v1".equalsIgnoreCase(key)) {
                    v1 = value;
                }
            }
        }

        if (ts == null || ts.isBlank() || v1 == null || v1.isBlank()) {
            log.warn("Encabezado x-signature malformado o incompleto.");
            return false;
        }

        // Validación de frescura del timestamp para prevenir ataques de replay
        if (maxToleranceSeconds > 0) {
            try {
                long timestampEpoch = Long.parseLong(ts);
                if (Math.abs(currentEpochSeconds - timestampEpoch) > maxToleranceSeconds) {
                    log.warn("Timestamp del webhook desfasado respecto al reloj local (replay attack potencial). ts: {}", ts);
                    return false;
                }
            } catch (NumberFormatException e) {
                log.warn("Timestamp ts no es un número válido: {}", ts);
                return false;
            }
        }

        String manifest = "id:" + dataId.trim().toLowerCase(Locale.ROOT) +
                ";request-id:" + xRequestId.trim() +
                ";ts:" + ts.trim() + ";";

        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);

            byte[] hmacBytes = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
            String calculatedHex = toHex(hmacBytes);

            byte[] calculatedBytes = calculatedHex.getBytes(StandardCharsets.UTF_8);
            byte[] expectedBytes = v1.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);

            // Comparación en tiempo constante para mitigar ataques de temporización (timing attacks)
            boolean matches = MessageDigest.isEqual(calculatedBytes, expectedBytes);
            if (!matches) {
                log.warn("Firma del webhook inválida para el recurso {}.", dataId);
            }
            return matches;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error al calcular el HMAC-SHA256 del webhook: {}", e.getMessage());
            return false;
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

}

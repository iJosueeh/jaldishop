package com.jaldishop.backend.payment.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MercadoPagoWebhookSignatureValidatorTest {

    private MercadoPagoWebhookSignatureValidator validator;
    private final String secretKey = "test_webhook_secret_key_12345";

    @BeforeEach
    void setUp() {
        validator = new MercadoPagoWebhookSignatureValidator();
    }

    private String calculateHmac(String manifest, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] bytes = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    @Test
    @DisplayName("Firma válida con manifest oficial coincide exitosamente")
    void shouldValidateCorrectSignatureSuccessfully() throws Exception {
        String dataId = "99887766";
        String requestId = "req-uuid-112233";
        long nowEpoch = Instant.now().getEpochSecond();
        String ts = String.valueOf(nowEpoch);

        String manifest = "id:" + dataId.toLowerCase() + ";request-id:" + requestId + ";ts:" + ts + ";";
        String v1Hash = calculateHmac(manifest, secretKey);

        String xSignature = "ts=" + ts + ",v1=" + v1Hash;

        boolean valid = validator.isValid(xSignature, requestId, dataId, secretKey);
        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("Firma inválida con hash incorrecto es rechazada")
    void shouldRejectInvalidSignatureHash() {
        String dataId = "99887766";
        String requestId = "req-uuid-112233";
        String ts = String.valueOf(Instant.now().getEpochSecond());
        String badSignature = "ts=" + ts + ",v1=0000000000000000000000000000000000000000000000000000000000000000";

        boolean valid = validator.isValid(badSignature, requestId, dataId, secretKey);
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Firma incompleta (falta ts o v1) es rechazada")
    void shouldRejectIncompleteSignatureHeader() {
        String dataId = "99887766";
        String requestId = "req-uuid-112233";

        assertThat(validator.isValid("ts=123456", requestId, dataId, secretKey)).isFalse();
        assertThat(validator.isValid("v1=abcdef", requestId, dataId, secretKey)).isFalse();
        assertThat(validator.isValid("", requestId, dataId, secretKey)).isFalse();
        assertThat(validator.isValid(null, requestId, dataId, secretKey)).isFalse();
    }

    @Test
    @DisplayName("Falta de x-request-id o data.id es rechazada")
    void shouldRejectMissingHeadersOrDataId() {
        String xSignature = "ts=123456,v1=abcdef";

        assertThat(validator.isValid(xSignature, null, "99887766", secretKey)).isFalse();
        assertThat(validator.isValid(xSignature, "", "99887766", secretKey)).isFalse();
        assertThat(validator.isValid(xSignature, "req-1", null, secretKey)).isFalse();
        assertThat(validator.isValid(xSignature, "req-1", "", secretKey)).isFalse();
        assertThat(validator.isValid(xSignature, "req-1", "99887766", null)).isFalse();
    }

    @Test
    @DisplayName("Timestamp desfasado (ataque de repetición/replay) es rechazado")
    void shouldRejectReplayAttackWithExpiredTimestamp() throws Exception {
        String dataId = "99887766";
        String requestId = "req-uuid-112233";
        long oldTsEpoch = Instant.now().getEpochSecond() - 3600; // 1 hora de desfase
        String ts = String.valueOf(oldTsEpoch);

        String manifest = "id:" + dataId.toLowerCase() + ";request-id:" + requestId + ";ts:" + ts + ";";
        String v1Hash = calculateHmac(manifest, secretKey);
        String xSignature = "ts=" + ts + ",v1=" + v1Hash;

        boolean valid = validator.isValid(xSignature, requestId, dataId, secretKey);
        assertThat(valid).isFalse();
    }

}

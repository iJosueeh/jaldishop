package com.jaldishop.backend.shared.security.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Servicio criptográfico de Field-Level Encryption (FLE).
 * Emplea AES-256-GCM para confidencialidad e integridad autenticada (AEAD)
 * y HMAC-SHA256 para Blind Indexing determinístico.
 */
@Service
public class CryptoService {

    private static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12; // 96 bits recomendados por NIST
    private static final int GCM_TAG_LENGTH_BITS = 128; // 16 bytes auth tag
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final SecretKey encryptionKey;
    private final SecretKey hmacKey;
    private final SecureRandom secureRandom;

    public CryptoService(
            @Value("${app.security.encryption.master-key:${JWT_SECRET:local-development-only-change-this-key-before-sharing-any-tokens-2026}}") String masterKeySecret
    ) {
        this.secureRandom = new SecureRandom();
        byte[] keyBytes = deriveKey(masterKeySecret, "AES-ENCRYPTION");
        this.encryptionKey = new SecretKeySpec(keyBytes, "AES");

        byte[] hmacKeyBytes = deriveKey(masterKeySecret, "HMAC-BLIND-INDEX");
        this.hmacKey = new SecretKeySpec(hmacKeyBytes, HMAC_SHA256);
    }

    /**
     * Cifra un texto plano usando AES-256-GCM con un IV aleatorio único.
     * Formato resultante: Base64(IV [12 bytes] + Ciphertext + Tag [16 bytes]).
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return plainText;
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, spec);

            byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherBytes.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherBytes);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Error al cifrar el campo sensible", e);
        }
    }

    /**
     * Descifra un texto en Base64 producido por {@link #encrypt(String)}.
     * Valida la integridad autenticada; falla si el texto o IV fue alterado.
     */
    public String decrypt(String cipherTextBase64) {
        if (cipherTextBase64 == null || cipherTextBase64.isBlank()) {
            return cipherTextBase64;
        }

        try {
            byte[] decoded;
            try {
                decoded = Base64.getDecoder().decode(cipherTextBase64);
            } catch (IllegalArgumentException e) {
                // Si no es un Base64 válido (ej. registros preexistentes en texto plano), retorna tal cual
                return cipherTextBase64;
            }

            if (decoded.length < GCM_IV_LENGTH_BYTES + (GCM_TAG_LENGTH_BITS / 8)) {
                // Demasiado corto para ser un paquete AES-GCM válido; fallback a texto original
                return cipherTextBase64;
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            byteBuffer.get(iv);

            byte[] cipherBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherBytes);

            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, spec);

            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            // Error de autenticación de tag o clave incorrecta
            throw new IllegalStateException("Fallo de integridad o clave al descifrar el campo sensible", e);
        }
    }

    /**
     * Genera un Blind Index (HMAC-SHA256) determinístico para permitir búsquedas
     * de igualdad en base de datos sin descifrar masivamente los registros.
     */
    public String blindIndex(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return null;
        }

        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(hmacKey);
            byte[] hash = mac.doFinal(plainText.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Error al calcular el blind index", e);
        }
    }

    /**
     * Enmascara un número de cuenta o CCI dejando visibles solo los últimos dígitos.
     */
    public static String mask(String sensitiveText, int visibleTailDigits) {
        if (sensitiveText == null || sensitiveText.isBlank()) {
            return sensitiveText;
        }
        String clean = sensitiveText.trim();
        if (clean.length() <= visibleTailDigits) {
            return "••••";
        }
        String tail = clean.substring(clean.length() - visibleTailDigits);
        return "•••• " + tail;
    }

    private static byte[] deriveKey(String secret, String purpose) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            sha256.update(purpose.getBytes(StandardCharsets.UTF_8));
            return sha256.digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo derivar la clave criptográfica", e);
        }
    }
}

package com.jaldishop.backend.shared.security.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CryptoServiceTest {

    private CryptoService cryptoService;

    @BeforeEach
    void setUp() {
        cryptoService = new CryptoService("clave-secreta-de-prueba-para-test-2026-super-segura");
    }

    @Test
    @DisplayName("Cifrado y descifrado de CCI bancario preserva el valor original")
    void encryptAndDecryptCci() {
        String cci = "002-194-001234567890-14";
        String encrypted = cryptoService.encrypt(cci);

        assertThat(encrypted).isNotNull();
        assertThat(encrypted).isNotEqualTo(cci);

        String decrypted = cryptoService.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(cci);
    }

    @Test
    @DisplayName("El cifrado produce ciphertexts distintos para el mismo texto plano (IV aleatorio único)")
    void differentCiphertextsForSameInput() {
        String phone = "987654321";
        String enc1 = cryptoService.encrypt(phone);
        String enc2 = cryptoService.encrypt(phone);

        assertThat(enc1).isNotEqualTo(enc2);
        assertThat(cryptoService.decrypt(enc1)).isEqualTo(phone);
        assertThat(cryptoService.decrypt(enc2)).isEqualTo(phone);
    }

    @Test
    @DisplayName("La manipulación del ciphertext dispara fallo de autenticación de tag en GCM")
    void tamperingTriggersAuthenticationFailure() {
        String cci = "002-194-001234567890-14";
        String encrypted = cryptoService.encrypt(cci);

        // Alterar un byte del texto cifrado
        byte[] bytes = java.util.Base64.getDecoder().decode(encrypted);
        bytes[bytes.length - 1] ^= 0x01;
        String tampered = java.util.Base64.getEncoder().encodeToString(bytes);

        assertThatThrownBy(() -> cryptoService.decrypt(tampered))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Fallo de integridad");
    }

    @Test
    @DisplayName("El Blind Index es determinístico para búsquedas exactas")
    void blindIndexIsDeterministic() {
        String cci = "00219400123456789014";
        String index1 = cryptoService.blindIndex(cci);
        String index2 = cryptoService.blindIndex(cci);

        assertThat(index1).isNotNull();
        assertThat(index1).isEqualTo(index2);
    }

    @Test
    @DisplayName("Enmascaramiento seguro de números")
    void maskingWorksCorrectly() {
        assertThat(CryptoService.mask("00219400123456789014", 4)).isEqualTo("•••• 9014");
        assertThat(CryptoService.mask("987654321", 4)).isEqualTo("•••• 4321");
        assertThat(CryptoService.mask("123", 4)).isEqualTo("••••");
        assertThat(CryptoService.mask(null, 4)).isNull();
    }
}

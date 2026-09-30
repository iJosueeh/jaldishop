package com.jaldishop.backend.shared.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PhoneUtilsTest {

    @Test
    @DisplayName("Normalizar número de 9 dígitos empezando en 9")
    void normalizeNineDigitsPhone() {
        assertEquals("+51987654321", PhoneUtils.normalizePeruPhone("987654321"));
        assertEquals("+51987654321", PhoneUtils.normalizePeruPhone(" 987654321 "));
    }

    @Test
    @DisplayName("Normalizar número de 11 dígitos con prefijo 519")
    void normalizeElevenDigitsPhone() {
        assertEquals("+51987654321", PhoneUtils.normalizePeruPhone("51987654321"));
    }

    @Test
    @DisplayName("Preservar número que ya tiene formato +519")
    void preserveAlreadyFormattedPhone() {
        assertEquals("+51987654321", PhoneUtils.normalizePeruPhone("+51987654321"));
        assertEquals("+51987654321", PhoneUtils.normalizePeruPhone("+51 987 654 321"));
    }

    @Test
    @DisplayName("Manejar números nulos o vacíos")
    void handleNullOrBlank() {
        assertNull(PhoneUtils.normalizePeruPhone(null));
        assertNull(PhoneUtils.normalizePeruPhone(""));
        assertNull(PhoneUtils.normalizePeruPhone("   "));
    }

    @Test
    @DisplayName("Preservar números internacionales distintos de Perú")
    void preserveInternationalPhone() {
        assertEquals("+50212345678", PhoneUtils.normalizePeruPhone("+50212345678"));
    }
}

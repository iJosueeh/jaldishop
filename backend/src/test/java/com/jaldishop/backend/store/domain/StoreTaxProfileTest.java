package com.jaldishop.backend.store.domain;

import com.jaldishop.backend.store.domain.enums.TaxIdType;
import com.jaldishop.backend.store.domain.enums.TaxRegime;
import com.jaldishop.backend.store.domain.model.StoreTaxProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StoreTaxProfileTest {

    @Test
    @DisplayName("DNI válido de 8 dígitos para negocio inicial")
    void testValidDniProfile() {
        StoreTaxProfile profile = StoreTaxProfile.createDniProfile("Juan Perez", "12345678");

        assertThat(profile.taxIdType()).isEqualTo(TaxIdType.DNI);
        assertThat(profile.taxIdNumber()).isEqualTo("12345678");
        assertThat(profile.taxRegime()).isEqualTo(TaxRegime.NONE);
        assertThat(profile.canIssueInvoices()).isFalse();
    }

    @Test
    @DisplayName("RUC 10 bajo Nuevo RUS (NRUS) fuerza canIssueInvoices en false")
    void testRusProfileNeverAllowsInvoices() {
        // Aunque intentemos pasar canIssueInvoices = true, la regla tributaria SUNAT debe forzarlo a false
        StoreTaxProfile profile = new StoreTaxProfile(
                "Picanteria Doña Luz",
                TaxIdType.RUC_10,
                "10456789012",
                TaxRegime.RUS,
                true // Intento de marcar true
        );

        assertThat(profile.canIssueInvoices()).isFalse();
        assertThat(profile.taxRegime()).isEqualTo(TaxRegime.RUS);
    }

    @Test
    @DisplayName("RUC 20 bajo Régimen MYPE o General sí permite emitir Facturas")
    void testMypeProfileAllowsInvoices() {
        StoreTaxProfile profile = StoreTaxProfile.createMypeProfile(
                "Gastronomia Gourmet SAC",
                TaxIdType.RUC_20,
                "20601234567",
                true
        );

        assertThat(profile.taxIdType()).isEqualTo(TaxIdType.RUC_20);
        assertThat(profile.taxIdNumber()).isEqualTo("20601234567");
        assertThat(profile.taxRegime()).isEqualTo(TaxRegime.MYPE);
        assertThat(profile.canIssueInvoices()).isTrue();
    }

    @Test
    @DisplayName("Rechaza RUC 10 inválido que no inicie con '10'")
    void testInvalidRuc10Prefix() {
        assertThatThrownBy(() -> new StoreTaxProfile(
                "Invalido",
                TaxIdType.RUC_10,
                "15456789012", // Prefijo erróneo
                TaxRegime.RER,
                true
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El RUC 10 debe tener 11 dígitos y comenzar con '10'.");
    }

    @Test
    @DisplayName("Rechaza RUC 20 inválido que no inicie con '20'")
    void testInvalidRuc20Prefix() {
        assertThatThrownBy(() -> new StoreTaxProfile(
                "Invalido SAC",
                TaxIdType.RUC_20,
                "25601234567", // Prefijo erróneo
                TaxRegime.MYPE,
                true
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El RUC 20 debe tener 11 dígitos y comenzar con '20'.");
    }

    @Test
    @DisplayName("Rechaza DNI con cantidad distinta a 8 dígitos")
    void testInvalidDniLength() {
        assertThatThrownBy(() -> StoreTaxProfile.createDniProfile("Juan", "1234567"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El DNI debe tener exactamente 8 dígitos.");
    }
}

package com.jaldishop.backend.store.domain;

import com.jaldishop.backend.store.domain.enums.TaxIdType;
import com.jaldishop.backend.store.domain.enums.TaxRegime;
import com.jaldishop.backend.store.domain.model.StoreTaxProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

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
        assertThat(profile.resolveEffectiveTaxRate(null)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("RUC 10 válido Módulo 11 bajo Nuevo RUS (NRUS) fuerza canIssueInvoices en false y tasa IGV 0.00")
    void testRusProfileNeverAllowsInvoicesOrIgv() {
        StoreTaxProfile profile = new StoreTaxProfile(
                "Picanteria Doña Luz",
                TaxIdType.RUC_10,
                "10456789019",
                TaxRegime.RUS,
                true
        );

        assertThat(profile.canIssueInvoices()).isFalse();
        assertThat(profile.taxRegime()).isEqualTo(TaxRegime.RUS);
        assertThat(profile.resolveEffectiveTaxRate(BigDecimal.ZERO)).isEqualByComparingTo("0.00");

        assertThatThrownBy(() -> profile.resolveEffectiveTaxRate(new BigDecimal("18.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nuevo RUS no discrimina IGV");
    }

    @Test
    @DisplayName("RUC 20 válido Módulo 11 bajo Régimen MYPE sí permite emitir Facturas e IGV 18%")
    void testMypeProfileAllowsInvoices() {
        StoreTaxProfile profile = StoreTaxProfile.createMypeProfile(
                "Gastronomia Gourmet SAC",
                TaxIdType.RUC_20,
                "20100070970",
                true
        );

        assertThat(profile.taxIdType()).isEqualTo(TaxIdType.RUC_20);
        assertThat(profile.taxIdNumber()).isEqualTo("20100070970");
        assertThat(profile.taxRegime()).isEqualTo(TaxRegime.MYPE);
        assertThat(profile.canIssueInvoices()).isTrue();
        assertThat(profile.resolveEffectiveTaxRate(null)).isEqualByComparingTo("18.00");
    }

    @Test
    @DisplayName("Rechaza RUC 10 o RUC 20 con dígito verificador Módulo 11 inválido")
    void testInvalidModulo11CheckDigit() {
        assertThatThrownBy(() -> new StoreTaxProfile(
                "Bodega Test",
                TaxIdType.RUC_10,
                "10456789012",
                TaxRegime.RUS,
                false
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Módulo 11");

        assertThatThrownBy(() -> new StoreTaxProfile(
                "Empresa SAC",
                TaxIdType.RUC_20,
                "20100070979",
                TaxRegime.GENERAL,
                true
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Módulo 11");
    }

    @Test
    @DisplayName("Rechaza combinaciones incompatibles TaxIdType x TaxRegime (RUC 20 en RUS o DNI en MYPE/RER/GENERAL)")
    void testIncompatibleRegimeAndDocType() {
        assertThatThrownBy(() -> new StoreTaxProfile(
                "Empresa SAC",
                TaxIdType.RUC_20,
                "20100070970",
                TaxRegime.RUS,
                false
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nuevo RUS");

        assertThatThrownBy(() -> new StoreTaxProfile(
                "Juan Perez",
                TaxIdType.DNI,
                "12345678",
                TaxRegime.MYPE,
                false
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requieren RUC 10 o RUC 20");
    }

    @Test
    @DisplayName("Rechaza RUC 20 sin Razón Social")
    void testRuc20RequiresLegalBusinessName() {
        assertThatThrownBy(() -> new StoreTaxProfile(
                "   ",
                TaxIdType.RUC_20,
                "20100070970",
                TaxRegime.RER,
                true
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("razón social es obligatoria");
    }

    @Test
    @DisplayName("Rechaza RUC 10 inválido que no inicie con '10'")
    void testInvalidRuc10Prefix() {
        assertThatThrownBy(() -> new StoreTaxProfile(
                "Invalido",
                TaxIdType.RUC_10,
                "15456789012",
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
                "25601234567",
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

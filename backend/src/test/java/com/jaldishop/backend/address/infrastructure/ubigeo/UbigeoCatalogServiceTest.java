package com.jaldishop.backend.address.infrastructure.ubigeo;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UbigeoCatalogServiceTest {

    private UbigeoCatalogService ubigeoCatalogService;

    @BeforeEach
    void setUp() {
        ubigeoCatalogService = new UbigeoCatalogService(new ObjectMapper());
        ubigeoCatalogService.init();
    }

    @Test
    @DisplayName("Carga exitosamente el catálogo oficial de INEI sin inventar códigos")
    void shouldLoadOfficialCatalogSuccessfully() {
        assertThat(ubigeoCatalogService.size()).isGreaterThanOrEqualTo(1800);
    }

    @Test
    @DisplayName("Valida códigos y jerarquías oficiales legítimas en todo el Perú")
    void shouldValidateLegitimateCodes() {
        // Miraflores - Lima - Lima
        assertThat(ubigeoCatalogService.isValidHierarchy("150122", "Lima", "Lima", "Miraflores")).isTrue();
        // Yanahuara - Arequipa - Arequipa
        assertThat(ubigeoCatalogService.isValidHierarchy("040126", "Arequipa", "Arequipa", "Yanahuara")).isTrue();
        // Chulucanas - Morropón - Piura
        assertThat(ubigeoCatalogService.isValidHierarchy("200401", "Piura", "Morropon", "Chulucanas")).isTrue();
    }

    @Test
    @DisplayName("Rechaza códigos inexistentes o inventados")
    void shouldRejectInvalidCodes() {
        assertThat(ubigeoCatalogService.isValidCode("999999")).isFalse();
        assertThat(ubigeoCatalogService.isValidCode("000000")).isFalse();
        assertThat(ubigeoCatalogService.isValidCode("12345")).isFalse();
    }

    @Test
    @DisplayName("Rechaza discrepancias entre código UBIGEO y nombres geográficos")
    void shouldRejectHierarchyMismatch() {
        // Código de Miraflores (Lima) pero con departamento Arequipa
        assertThat(ubigeoCatalogService.isValidHierarchy("150122", "Arequipa", "Lima", "Miraflores")).isFalse();
    }
}

package com.jaldishop.backend.store.domain.model;

import com.jaldishop.backend.store.domain.enums.TaxIdType;
import com.jaldishop.backend.store.domain.enums.TaxRegime;

import java.util.Objects;

/**
 * Value Object inmutable que representa el perfil fiscal y tributario de una tienda en Perú.
 * Reglas de negocio:
 * - Los comercios en el Nuevo RUS (NRUS) solo pueden emitir Boletas de Venta; nunca Facturas.
 * - Validación estricta de formato: DNI (8 dígitos), RUC 10 (11 dígitos, prefijo 10), RUC 20 (11 dígitos, prefijo 20).
 */
public record StoreTaxProfile(
        String legalBusinessName,
        TaxIdType taxIdType,
        String taxIdNumber,
        TaxRegime taxRegime,
        boolean canIssueInvoices
) {
    public StoreTaxProfile {
        taxIdType = taxIdType != null ? taxIdType : TaxIdType.DNI;
        taxRegime = taxRegime != null ? taxRegime : TaxRegime.RUS;

        if (taxIdNumber != null && !taxIdNumber.isBlank()) {
            taxIdNumber = taxIdNumber.trim();
            validateTaxId(taxIdType, taxIdNumber);
        }

        // Regla SUNAT: Si está en el régimen RUS, NO puede emitir facturas bajo ninguna circunstancia.
        if (taxRegime == TaxRegime.RUS || taxRegime == TaxRegime.NONE) {
            canIssueInvoices = false;
        }
    }

    public static StoreTaxProfile createDniProfile(String legalName, String dni) {
        return new StoreTaxProfile(legalName, TaxIdType.DNI, dni, TaxRegime.NONE, false);
    }

    public static StoreTaxProfile createRusProfile(String legalName, String ruc10) {
        return new StoreTaxProfile(legalName, TaxIdType.RUC_10, ruc10, TaxRegime.RUS, false);
    }

    public static StoreTaxProfile createMypeProfile(String legalName, TaxIdType type, String ruc, boolean canIssueInvoices) {
        return new StoreTaxProfile(legalName, type, ruc, TaxRegime.MYPE, canIssueInvoices);
    }

    private static void validateTaxId(TaxIdType type, String number) {
        if (!number.matches("\\d+")) {
            throw new IllegalArgumentException("El documento fiscal debe contener únicamente dígitos numéricos.");
        }

        switch (type) {
            case DNI -> {
                if (number.length() != 8) {
                    throw new IllegalArgumentException("El DNI debe tener exactamente 8 dígitos.");
                }
            }
            case RUC_10 -> {
                if (number.length() != 11 || !number.startsWith("10")) {
                    throw new IllegalArgumentException("El RUC 10 debe tener 11 dígitos y comenzar con '10'.");
                }
            }
            case RUC_20 -> {
                if (number.length() != 11 || !number.startsWith("20")) {
                    throw new IllegalArgumentException("El RUC 20 debe tener 11 dígitos y comenzar con '20'.");
                }
            }
        }
    }
}

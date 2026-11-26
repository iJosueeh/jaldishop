package com.jaldishop.backend.store.domain.model;

import com.jaldishop.backend.store.domain.enums.TaxIdType;
import com.jaldishop.backend.store.domain.enums.TaxRegime;
import com.jaldishop.backend.store.domain.validation.SunatRucValidator;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Value Object inmutable que representa el perfil fiscal y tributario de una tienda en Perú.
 * Reglas de negocio SUNAT:
 * - Nuevo RUS (RUS): solo personas naturales (RUC 10 o DNI en formalización), nunca RUC 20,
 *   no discrimina IGV (taxRate = 0.00) y nunca emite facturas (canIssueInvoices = false).
 * - RER, MYPE y GENERAL: requieren RUC 10 o RUC 20 válido (Módulo 11). RUC 20 exige Razón Social.
 *   Afecto a IGV 18.00% por defecto salvo exoneración explícita.
 */
public record StoreTaxProfile(
        String legalBusinessName,
        TaxIdType taxIdType,
        String taxIdNumber,
        TaxRegime taxRegime,
        boolean canIssueInvoices
) {
    private static final BigDecimal ZERO_IGV = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal STANDARD_IGV = new BigDecimal("18.00");

    public StoreTaxProfile {
        legalBusinessName = (legalBusinessName != null && !legalBusinessName.isBlank())
                ? legalBusinessName.trim()
                : null;
        taxIdType = taxIdType != null ? taxIdType : TaxIdType.DNI;
        taxRegime = taxRegime != null ? taxRegime : TaxRegime.RUS;
        taxIdNumber = (taxIdNumber != null && !taxIdNumber.isBlank())
                ? taxIdNumber.trim()
                : null;

        validateCompatibility(taxIdType, taxRegime);

        if (taxIdNumber != null) {
            validateTaxId(taxIdType, taxIdNumber);
            if (taxIdType == TaxIdType.RUC_20 && legalBusinessName == null) {
                throw new IllegalArgumentException("La razón social es obligatoria para comercios con RUC 20.");
            }
        }

        if (taxRegime == TaxRegime.RUS || taxRegime == TaxRegime.NONE) {
            canIssueInvoices = false;
        }
    }

    public static StoreTaxProfile createValidated(
            String legalBusinessName,
            TaxIdType taxIdType,
            String taxIdNumber,
            TaxRegime taxRegime,
            boolean canIssueInvoices
    ) {
        if (taxIdType == null) {
            throw new IllegalArgumentException("El tipo de documento fiscal es obligatorio.");
        }
        if (taxRegime == null) {
            throw new IllegalArgumentException("El régimen tributario SUNAT es obligatorio.");
        }
        if (taxIdNumber == null || taxIdNumber.isBlank()) {
            throw new IllegalArgumentException("El número de documento fiscal (DNI o RUC) es obligatorio.");
        }
        if (legalBusinessName == null || legalBusinessName.isBlank()) {
            throw new IllegalArgumentException("La razón social o nombre del titular es obligatorio.");
        }
        return new StoreTaxProfile(legalBusinessName, taxIdType, taxIdNumber, taxRegime, canIssueInvoices);
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

    public BigDecimal resolveEffectiveTaxRate(BigDecimal requestedTaxRate) {
        if (requestedTaxRate != null && (requestedTaxRate.compareTo(BigDecimal.ZERO) < 0 || requestedTaxRate.compareTo(new BigDecimal("100.00")) > 0)) {
            throw new IllegalArgumentException("La tasa de impuestos (IGV) debe estar entre 0.00% y 100.00%.");
        }
        if (taxRegime == TaxRegime.RUS || taxRegime == TaxRegime.NONE) {
            if (requestedTaxRate != null && requestedTaxRate.compareTo(BigDecimal.ZERO) > 0) {
                throw new IllegalArgumentException("El Nuevo RUS no discrimina IGV; la tasa de impuesto debe ser 0.00%.");
            }
            return ZERO_IGV;
        }
        if (requestedTaxRate == null) {
            return STANDARD_IGV;
        }
        return requestedTaxRate.setScale(2, RoundingMode.HALF_UP);
    }

    private static void validateCompatibility(TaxIdType type, TaxRegime regime) {
        switch (regime) {
            case RUS -> {
                if (type == TaxIdType.RUC_20) {
                    throw new IllegalArgumentException("El Nuevo RUS solo aplica a personas naturales con RUC 10 o DNI en formalización.");
                }
            }
            case NONE -> {
                if (type != TaxIdType.DNI) {
                    throw new IllegalArgumentException("Sin régimen fiscal solo se admite DNI.");
                }
            }
            case RER, MYPE, GENERAL -> {
                if (type == TaxIdType.DNI) {
                    throw new IllegalArgumentException("Los regímenes RER, MYPE Tributario y General requieren RUC 10 o RUC 20.");
                }
            }
        }
    }

    private static void validateTaxId(TaxIdType type, String number) {
        if (!number.matches("\\d+")) {
            throw new IllegalArgumentException("El documento fiscal debe contener únicamente dígitos numéricos.");
        }

        switch (type) {
            case DNI -> {
                if (!SunatRucValidator.isValidDni(number)) {
                    throw new IllegalArgumentException("El DNI debe tener exactamente 8 dígitos.");
                }
            }
            case RUC_10 -> {
                if (number.length() != 11 || !number.startsWith("10")) {
                    throw new IllegalArgumentException("El RUC 10 debe tener 11 dígitos y comenzar con '10'.");
                }
                if (!SunatRucValidator.hasValidModulo11CheckDigit(number)) {
                    throw new IllegalArgumentException("El RUC 10 ingresado no tiene un dígito verificador SUNAT válido (Módulo 11).");
                }
            }
            case RUC_20 -> {
                if (number.length() != 11 || !number.startsWith("20")) {
                    throw new IllegalArgumentException("El RUC 20 debe tener 11 dígitos y comenzar con '20'.");
                }
                if (!SunatRucValidator.hasValidModulo11CheckDigit(number)) {
                    throw new IllegalArgumentException("El RUC 20 ingresado no tiene un dígito verificador SUNAT válido (Módulo 11).");
                }
            }
        }
    }
}

package com.jaldishop.backend.store.domain.validation;

/**
 * Validador de documentos tributarios peruanos (SUNAT / RENIEC).
 * Implementa el algoritmo oficial Módulo 11 de SUNAT para RUC de 11 dígitos.
 */
public final class SunatRucValidator {

    private static final int[] RUC_WEIGHTS = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

    private SunatRucValidator() {
    }

    public static boolean isValidDni(String dni) {
        return dni != null && dni.matches("^\\d{8}$");
    }

    public static boolean isValidRuc10(String ruc) {
        return isValidRucWithPrefix(ruc, "10");
    }

    public static boolean isValidRuc20(String ruc) {
        return isValidRucWithPrefix(ruc, "20");
    }

    public static boolean isValidRuc(String ruc) {
        return isValidRuc10(ruc) || isValidRuc20(ruc);
    }

    private static boolean isValidRucWithPrefix(String ruc, String prefix) {
        if (ruc == null || !ruc.matches("^\\d{11}$") || !ruc.startsWith(prefix)) {
            return false;
        }
        return hasValidModulo11CheckDigit(ruc);
    }

    public static boolean hasValidModulo11CheckDigit(String ruc) {
        if (ruc == null || !ruc.matches("^\\d{11}$")) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += (ruc.charAt(i) - '0') * RUC_WEIGHTS[i];
        }
        int remainder = 11 - (sum % 11);
        int expectedDigit = switch (remainder) {
            case 10 -> 0;
            case 11 -> 1;
            default -> remainder;
        };
        int actualDigit = ruc.charAt(10) - '0';
        return actualDigit == expectedDigit;
    }
}

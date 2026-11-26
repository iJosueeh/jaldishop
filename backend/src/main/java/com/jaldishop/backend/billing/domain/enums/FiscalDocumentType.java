package com.jaldishop.backend.billing.domain.enums;

/**
 * Código Catálogo N° 01 de SUNAT para Comprobantes de Pago.
 */
public enum FiscalDocumentType {
    FACTURA("01", "F001", "Factura Electrónica"),
    BOLETA("03", "B001", "Boleta de Venta"),
    NOTA_VENTA("00", "NV01", "Nota de Venta / Ticket");

    private final String sunatCode;
    private final String defaultSeries;
    private final String displayName;

    FiscalDocumentType(String sunatCode, String defaultSeries, String displayName) {
        this.sunatCode = sunatCode;
        this.defaultSeries = defaultSeries;
        this.displayName = displayName;
    }

    public String getSunatCode() {
        return sunatCode;
    }

    public String getDefaultSeries() {
        return defaultSeries;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static FiscalDocumentType fromSunatCode(String code) {
        for (FiscalDocumentType type : values()) {
            if (type.sunatCode.equals(code)) {
                return type;
            }
        }
        return NOTA_VENTA;
    }
}

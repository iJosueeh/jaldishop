package com.jaldishop.backend.store.web.dto;

import com.jaldishop.backend.store.domain.enums.TaxIdType;
import com.jaldishop.backend.store.domain.enums.TaxRegime;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateStoreTaxProfileRequest(
        @Size(max = 200, message = "La razón social no debe exceder 200 caracteres")
        String legalBusinessName,

        TaxIdType taxIdType,

        @Size(max = 11, message = "El número de documento fiscal no debe exceder 11 caracteres")
        String taxIdNumber,

        TaxRegime taxRegime,

        boolean canIssueInvoices,

        @DecimalMin("0.00") @DecimalMax("100.00")
        BigDecimal taxRate
) {
    public UpdateStoreTaxProfileRequest(
            String legalBusinessName,
            TaxIdType taxIdType,
            String taxIdNumber,
            TaxRegime taxRegime,
            boolean canIssueInvoices
    ) {
        this(legalBusinessName, taxIdType, taxIdNumber, taxRegime, canIssueInvoices, null);
    }
}

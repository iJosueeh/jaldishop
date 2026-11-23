package com.jaldishop.backend.store.web.dto;

import com.jaldishop.backend.store.domain.enums.TaxIdType;
import com.jaldishop.backend.store.domain.enums.TaxRegime;
import jakarta.validation.constraints.Size;

public record UpdateStoreTaxProfileRequest(
        @Size(max = 200, message = "La razón social no debe exceder 200 caracteres")
        String legalBusinessName,

        TaxIdType taxIdType,

        @Size(max = 11, message = "El número de documento fiscal no debe exceder 11 caracteres")
        String taxIdNumber,

        TaxRegime taxRegime,

        boolean canIssueInvoices
) {}

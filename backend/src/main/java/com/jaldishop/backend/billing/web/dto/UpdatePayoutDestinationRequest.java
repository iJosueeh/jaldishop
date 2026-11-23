package com.jaldishop.backend.billing.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdatePayoutDestinationRequest(
        @NotBlank(message = "El canal de liquidación es obligatorio")
        @Pattern(regexp = "BANK_TRANSFER_CCI|YAPE_PHONE|PLIN_PHONE", message = "Canal no válido")
        String payoutChannel,

        String bankName,
        String accountNumber,

        @Pattern(regexp = "^$|\\d{20}", message = "El CCI debe tener exactamente 20 dígitos numéricos")
        String accountCci,

        @Pattern(regexp = "^$|9\\d{8}", message = "El teléfono debe tener 9 dígitos y empezar con 9")
        String phoneNumber,

        @NotBlank(message = "El nombre del titular es obligatorio")
        @Size(max = 150)
        String accountHolderName,

        @NotBlank(message = "El tipo de documento del titular es obligatorio")
        @Pattern(regexp = "DNI|RUC|CE", message = "Tipo de documento no válido")
        String accountHolderDocType,

        @NotBlank(message = "El número de documento del titular es obligatorio")
        @Size(min = 8, max = 11, message = "El documento debe tener entre 8 y 11 dígitos")
        String accountHolderDocNumber
) {}

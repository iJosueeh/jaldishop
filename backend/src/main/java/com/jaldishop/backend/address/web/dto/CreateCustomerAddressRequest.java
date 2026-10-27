package com.jaldishop.backend.address.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateCustomerAddressRequest(
        @NotBlank(message = "El alias es obligatorio.")
        @Size(max = 50, message = "El alias no puede exceder los 50 caracteres.")
        String alias,

        @NotBlank(message = "El nombre del destinatario es obligatorio.")
        @Size(max = 200, message = "El nombre del destinatario no puede exceder los 200 caracteres.")
        String recipientName,

        @NotBlank(message = "El teléfono de contacto es obligatorio.")
        @Pattern(regexp = "^9\\d{8}$", message = "El teléfono debe ser un número celular peruano válido de 9 dígitos que comience con 9.")
        String recipientPhone,

        @NotBlank(message = "La dirección exacta es obligatoria.")
        @Size(max = 255, message = "La dirección exacta no puede exceder los 255 caracteres.")
        String streetAddress,

        @NotBlank(message = "El departamento es obligatorio.")
        @Size(max = 100, message = "El departamento no puede exceder los 100 caracteres.")
        String department,

        @NotBlank(message = "La provincia es obligatoria.")
        @Size(max = 100, message = "La provincia no puede exceder los 100 caracteres.")
        String province,

        @NotBlank(message = "El distrito es obligatorio.")
        @Size(max = 100, message = "El distrito no puede exceder los 100 caracteres.")
        String district,

        @NotBlank(message = "El código UBIGEO es obligatorio.")
        @Pattern(regexp = "^\\d{6}$", message = "El código UBIGEO debe constar de exactamente 6 dígitos numéricos.")
        String ubigeoCode,

        @Size(max = 300, message = "La referencia no puede exceder los 300 caracteres.")
        String reference,

        BigDecimal latitude,
        BigDecimal longitude,
        boolean isDefault
) {}

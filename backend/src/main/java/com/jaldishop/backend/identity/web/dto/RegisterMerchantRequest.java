package com.jaldishop.backend.identity.web.dto;

import com.jaldishop.backend.shared.validation.ValidPhone;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record RegisterMerchantRequest(
        @Email @NotBlank @Size(max = 254, message = "El correo no debe exceder 254 caracteres") String email,
        @NotBlank @Size(min = 6) String password,
        @NotBlank @Size(max = 100, message = "El nombre no debe exceder 100 caracteres") String firstName,
        @NotBlank @Size(max = 100, message = "El apellido no debe exceder 100 caracteres") String lastName,
        @ValidPhone String phone,
        @NotBlank @Size(min = 2, max = 160) String storeName,
        String businessType,
        @ValidPhone String storeContactPhone,
        String address,
        String addressReference,
        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        boolean pickupEnabled,
        boolean deliveryEnabled,
        String logoUrl,
        String bannerUrl,
        Set<UUID> categoryIds,
        @NotBlank(message = "La razón social o nombre del titular es obligatorio")
        @Size(max = 200, message = "La razón social no debe exceder 200 caracteres")
        String legalBusinessName,
        @NotBlank(message = "El tipo de documento fiscal es obligatorio")
        String taxIdType,
        @NotBlank(message = "El número de documento fiscal (DNI o RUC) es obligatorio")
        @Size(min = 8, max = 11, message = "El documento fiscal debe tener entre 8 y 11 dígitos")
        String taxIdNumber,
        @NotBlank(message = "El régimen tributario SUNAT es obligatorio")
        String taxRegime,
        boolean canIssueInvoices,
        @DecimalMin("0.00") @DecimalMax("100.00")
        BigDecimal taxRate,
        @AssertTrue(message = "Debes aceptar los Términos de Uso y Declaración de Datos Tributarios SUNAT")
        boolean sunatTermsAccepted
) {
    public RegisterMerchantRequest(
            String email,
            String password,
            String firstName,
            String lastName,
            String phone,
            String storeName,
            String businessType,
            String storeContactPhone,
            String address,
            String addressReference,
            BigDecimal latitude,
            BigDecimal longitude,
            boolean pickupEnabled,
            boolean deliveryEnabled,
            String logoUrl,
            String bannerUrl,
            Set<UUID> categoryIds
    ) {
        this(
                email, password, firstName, lastName, phone, storeName, businessType,
                storeContactPhone, address, addressReference, latitude, longitude,
                pickupEnabled, deliveryEnabled, logoUrl, bannerUrl, categoryIds,
                null, null, null, null, false, null, false
        );
    }

    public RegisterMerchantRequest(
            String email,
            String password,
            String firstName,
            String lastName,
            String phone,
            String storeName,
            String businessType,
            String storeContactPhone,
            String address,
            boolean pickupEnabled,
            boolean deliveryEnabled,
            String logoUrl,
            String bannerUrl,
            Set<UUID> categoryIds
    ) {
        this(
                email, password, firstName, lastName, phone, storeName, businessType,
                storeContactPhone, address, null, null, null,
                pickupEnabled, deliveryEnabled, logoUrl, bannerUrl, categoryIds,
                null, null, null, null, false, null, false
        );
    }

    public RegisterMerchantRequest(
            String email,
            String password,
            String firstName,
            String lastName,
            String phone,
            String storeName,
            String businessType,
            String storeContactPhone,
            String address,
            boolean pickupEnabled,
            boolean deliveryEnabled
    ) {
        this(
                email, password, firstName, lastName, phone, storeName, businessType,
                storeContactPhone, address, null, null, null,
                pickupEnabled, deliveryEnabled, null, null, null,
                null, null, null, null, false, null, false
        );
    }
}

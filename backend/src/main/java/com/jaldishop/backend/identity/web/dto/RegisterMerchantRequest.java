package com.jaldishop.backend.identity.web.dto;

import com.jaldishop.backend.shared.validation.ValidPhone;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record RegisterMerchantRequest(
   @Email @NotBlank String email,
   @NotBlank @Size(min = 6) String password,
   @NotBlank String firstName,
   @NotBlank String lastName,
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
   Set<UUID> categoryIds
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
           boolean pickupEnabled,
           boolean deliveryEnabled,
           String logoUrl,
           String bannerUrl,
           Set<UUID> categoryIds
   ) {
       this(
               email,
               password,
               firstName,
               lastName,
               phone,
               storeName,
               businessType,
               storeContactPhone,
               address,
               null,
               null,
               null,
               pickupEnabled,
               deliveryEnabled,
               logoUrl,
               bannerUrl,
               categoryIds
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
               email,
               password,
               firstName,
               lastName,
               phone,
               storeName,
               businessType,
               storeContactPhone,
               address,
               null,
               null,
               null,
               pickupEnabled,
               deliveryEnabled,
               null,
               null,
               null
       );
   }
}

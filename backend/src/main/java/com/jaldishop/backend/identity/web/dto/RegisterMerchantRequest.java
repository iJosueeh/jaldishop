package com.jaldishop.backend.identity.web.dto;

import com.jaldishop.backend.shared.validation.ValidPhone;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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
   boolean pickupEnabled,
   boolean deliveryEnabled
) {}

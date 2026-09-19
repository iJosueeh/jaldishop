package com.jaldishop.backend.identity.web.dto;

import com.jaldishop.backend.shared.validation.ValidPhone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
   @NotBlank(message = "El nombre es obligatorio") @Size(max = 100) String firstName,
   @NotBlank(message = "El apellido es obligatorio") @Size(max = 100) String lastName,
   @ValidPhone String phone
) {}

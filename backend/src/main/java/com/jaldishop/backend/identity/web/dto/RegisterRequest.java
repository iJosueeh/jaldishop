package com.jaldishop.backend.identity.web.dto;

import com.jaldishop.backend.shared.validation.ValidPhone;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 6) String password,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @ValidPhone String phone
) {}

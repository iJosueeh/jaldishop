package com.jaldishop.backend.identity.web.dto;

import com.jaldishop.backend.shared.validation.ValidPhone;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InviteAdminRequest(
        @Email @NotBlank @Size(max = 254) String email,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @ValidPhone String phone
) {}

package com.jaldishop.backend.shared.domain.model;

public record CustomerSnapshot(
        String name,
        String phone,
        String email
) {
    public CustomerSnapshot {
        name = name != null ? name.trim() : null;
        phone = phone != null ? phone.trim() : null;
        email = email != null ? email.trim() : null;
    }

    public static CustomerSnapshot of(String name, String phone, String email) {
        return new CustomerSnapshot(name, phone, email);
    }
}

package com.jaldishop.backend.shared.util;

public final class PhoneUtils {

    private PhoneUtils() {
    }

    public static String normalizePeruPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String trimmed = phone.trim();
        String digitsOnly = trimmed.replaceAll("\\D", "");

        if (digitsOnly.length() == 9 && digitsOnly.startsWith("9")) {
            return "+51" + digitsOnly;
        }
        if (digitsOnly.length() == 11 && digitsOnly.startsWith("519")) {
            return "+" + digitsOnly;
        }
        return trimmed;
    }
}

package com.jaldishop.backend.shared.security;

import java.util.regex.Pattern;

/**
 * Plain-text sanitizer for user-authored configuration text (notices, policies).
 * Strips HTML tags and dangerous control characters while preserving normal punctuation.
 */
public final class TextSanitizer {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern CONTROL_CHARS_PATTERN = Pattern.compile("[\\p{Cntrl}&&[^\r\n\t]]");

    private TextSanitizer() {
    }

    public static String sanitizePlainText(String input) {
        if (input == null) {
            return null;
        }
        String withoutTags = HTML_TAG_PATTERN.matcher(input).replaceAll("");
        String withoutControl = CONTROL_CHARS_PATTERN.matcher(withoutTags).replaceAll("");
        return withoutControl.trim();
    }
}

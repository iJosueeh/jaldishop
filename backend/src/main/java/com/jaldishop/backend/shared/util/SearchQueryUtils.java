package com.jaldishop.backend.shared.util;

import java.util.Locale;

/**
 * Utilidades centralizadas para normalización y sanitización de términos de búsqueda SQL LIKE.
 */
public final class SearchQueryUtils {

    private SearchQueryUtils() {
    }

    /**
     * Normaliza un texto de búsqueda:
     * - Retorna {@code null} si es nulo o está en blanco.
     * - Remueve espacios iniciales y finales.
     * - Colapsa múltiples espacios consecutivos en un único espacio.
     */
    public static String normalize(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        return input.trim().replaceAll("\\s+", " ");
    }

    /**
     * Escapa caracteres especiales de SQL LIKE ('\\', '%', '_') utilizando '\\' como carácter de escape.
     * La barra invertida '\\' debe reemplazarse primero para evitar doble escape.
     */
    public static String escapeSqlLike(String input) {
        if (input == null) {
            return null;
        }
        return input
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    /**
     * Construye un patrón SQL LIKE de coincidencia parcial en minúsculas: {@code %término_escapado%}.
     * Retorna {@code null} si el término normalizado es nulo o está en blanco.
     */
    public static String toSqlLikeContainsPattern(String input) {
        String normalized = normalize(input);
        if (normalized == null) {
            return null;
        }
        String escaped = escapeSqlLike(normalized);
        return "%" + escaped.toLowerCase(Locale.ROOT) + "%";
    }
}

package com.jaldishop.backend.shared.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class SearchQueryUtilsTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n", "\r\n"})
    @DisplayName("normalize retorna null ante cadenas nulas o en blanco")
    void normalizeReturnsNullForBlank(String input) {
        assertThat(SearchQueryUtils.normalize(input)).isNull();
    }

    @Test
    @DisplayName("normalize recorta extremos y colapsa espacios intermedios")
    void normalizeTrimsAndCollapsesSpaces() {
        assertThat(SearchQueryUtils.normalize("  Juan   Carlos   Mendoza  "))
                .isEqualTo("Juan Carlos Mendoza");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("toSqlLikeContainsPattern retorna null ante cadenas nulas o en blanco")
    void toSqlLikeContainsPatternReturnsNullForBlank(String input) {
        assertThat(SearchQueryUtils.toSqlLikeContainsPattern(input)).isNull();
    }

    @Test
    @DisplayName("escapeSqlLike escapa barras invertidas, porcentajes y guiones bajos")
    void escapeSqlLikeEscapesSpecialChars() {
        assertThat(SearchQueryUtils.escapeSqlLike(null)).isNull();
        assertThat(SearchQueryUtils.escapeSqlLike("100%")).isEqualTo("100\\%");
        assertThat(SearchQueryUtils.escapeSqlLike("order_123")).isEqualTo("order\\_123");
        assertThat(SearchQueryUtils.escapeSqlLike("path\\to\\file")).isEqualTo("path\\\\to\\\\file");
        assertThat(SearchQueryUtils.escapeSqlLike("100%_val\\id")).isEqualTo("100\\%\\_val\\\\id");
    }

    @Test
    @DisplayName("toSqlLikeContainsPattern normaliza, escapa y convierte a minúsculas")
    void toSqlLikeContainsPatternTransformsCorrectly() {
        assertThat(SearchQueryUtils.toSqlLikeContainsPattern("  JAL_2026%01\\A  "))
                .isEqualTo("%jal\\_2026\\%01\\\\a%");

        assertThat(SearchQueryUtils.toSqlLikeContainsPattern("Carlos Mendoza"))
                .isEqualTo("%carlos mendoza%");
    }

    @Test
    @DisplayName("toSqlLikeContainsPattern preserva caracteres Unicode")
    void toSqlLikeContainsPatternPreservesUnicode() {
        assertThat(SearchQueryUtils.toSqlLikeContainsPattern("María Ñandú"))
                .isEqualTo("%maría ñandú%");
    }
}

package com.jaldishop.backend.notification.infrastructure.persistence;
import com.jaldishop.backend.notification.application.command.*;
import com.jaldishop.backend.notification.application.service.*;
import com.jaldishop.backend.notification.domain.model.*;
import com.jaldishop.backend.notification.domain.enums.*;
import com.jaldishop.backend.notification.domain.repository.*;
import com.jaldishop.backend.notification.domain.port.*;


import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit Tests: NotificationDeduplicationExceptionClassifier")
class NotificationDeduplicationExceptionClassifierTest {

    @Test
    @DisplayName("Debe clasificar como violación de deduplicación cuando el constraint y SQLState coinciden exactamente")
    void shouldClassifyTrueWhenConstraintAndSqlStateMatch() {
        SQLException sqlEx = new SQLException("duplicate key value violates unique constraint", "23505");
        ConstraintViolationException cve = new ConstraintViolationException(
                "could not execute statement",
                sqlEx,
                sqlEx.getMessage(),
                "uq_notifications_deduplication_key"
        );

        boolean isDedup = NotificationDeduplicationExceptionClassifier.isDeduplicationViolation(cve);
        assertThat(isDedup).isTrue();
    }

    @Test
    @DisplayName("Debe clasificar como FALSE cuando el SQLState es 23505 pero el constraint es distinto (otra clave única)")
    void shouldClassifyFalseWhenConstraintIsDifferent() {
        SQLException sqlEx = new SQLException("duplicate key", "23505");
        ConstraintViolationException cve = new ConstraintViolationException(
                "could not execute statement",
                sqlEx,
                sqlEx.getMessage(),
                "uq_users_email"
        );

        boolean isDedup = NotificationDeduplicationExceptionClassifier.isDeduplicationViolation(cve);
        assertThat(isDedup).isFalse();
    }

    @Test
    @DisplayName("Debe clasificar como FALSE cuando es una violación de clave foránea (SQLState 23503)")
    void shouldClassifyFalseWhenForeignKeyViolation() {
        SQLException sqlEx = new SQLException("foreign key violation", "23503");
        ConstraintViolationException cve = new ConstraintViolationException(
                "could not execute statement",
                sqlEx,
                sqlEx.getMessage(),
                "fk_notifications_user_id"
        );

        boolean isDedup = NotificationDeduplicationExceptionClassifier.isDeduplicationViolation(cve);
        assertThat(isDedup).isFalse();
    }

    @Test
    @DisplayName("Debe clasificar como FALSE para excepciones no relacionadas (ej. NullPointerException)")
    void shouldClassifyFalseForUnrelatedExceptions() {
        Exception ex = new RuntimeException("DB Connection timeout");
        boolean isDedup = NotificationDeduplicationExceptionClassifier.isDeduplicationViolation(ex);
        assertThat(isDedup).isFalse();
    }

}

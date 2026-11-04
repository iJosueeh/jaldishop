package com.jaldishop.backend.notification.infrastructure.persistence;

import org.hibernate.exception.ConstraintViolationException;

import java.lang.reflect.Method;
import java.sql.SQLException;

public final class NotificationDeduplicationExceptionClassifier {

    public static final String SQL_STATE_UNIQUE_VIOLATION = "23505";
    public static final String DEDUP_CONSTRAINT_NAME = "uq_notifications_deduplication_key";

    private NotificationDeduplicationExceptionClassifier() {}

    public static boolean isDeduplicationViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConstraintViolationException cve) {
                if (SQL_STATE_UNIQUE_VIOLATION.equals(cve.getSQLState())
                        && DEDUP_CONSTRAINT_NAME.equalsIgnoreCase(cve.getConstraintName())) {
                    return true;
                }
            }

            if (current instanceof SQLException sqlEx) {
                if (SQL_STATE_UNIQUE_VIOLATION.equals(sqlEx.getSQLState())) {
                    String constraint = extractConstraintFromPostgreSqlException(sqlEx);
                    if (constraint != null && DEDUP_CONSTRAINT_NAME.equalsIgnoreCase(constraint)) {
                        return true;
                    }
                }
            }

            if ("org.postgresql.util.PSQLException".equals(current.getClass().getName())) {
                String constraint = extractConstraintFromPostgreSqlException(current);
                if (constraint != null && DEDUP_CONSTRAINT_NAME.equalsIgnoreCase(constraint)) {
                    return true;
                }
            }

            current = current.getCause();
        }
        return false;
    }

    private static String extractConstraintFromPostgreSqlException(Object target) {
        try {
            Method getServerErrorMessageMethod = target.getClass().getMethod("getServerErrorMessage");
            Object serverError = getServerErrorMessageMethod.invoke(target);
            if (serverError != null) {
                Method getConstraintMethod = serverError.getClass().getMethod("getConstraint");
                Object constraint = getConstraintMethod.invoke(serverError);
                return constraint != null ? constraint.toString() : null;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

}

package com.jaldishop.backend.analytics.infrastructure.persistence;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Adaptador de compatibilidad que extiende la implementación canónica en el subpaquete adapter.
 * Sigue la convención del monolito com.jaldishop.backend.*.infrastructure.persistence.adapter.
 */
public class JdbcAnalyticsRepositoryAdapter extends com.jaldishop.backend.analytics.infrastructure.persistence.adapter.JdbcAnalyticsRepositoryAdapter {

    public JdbcAnalyticsRepositoryAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }
}

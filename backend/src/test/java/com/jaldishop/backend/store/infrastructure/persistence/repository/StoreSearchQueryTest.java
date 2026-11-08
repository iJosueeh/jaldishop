package com.jaldishop.backend.store.infrastructure.persistence.repository;

import com.jaldishop.backend.store.domain.enums.StoreStatus;
import com.jaldishop.backend.store.infrastructure.persistence.entity.StoreEntity;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import static org.assertj.core.api.Assertions.assertThat;

class StoreSearchQueryTest {
    @Test
    void hibernateAcceptsTheSearchExpressionWithTypedParameters() throws Exception {
        var config = new Configuration()
                .addAnnotatedClass(StoreEntity.class)
                .addAnnotatedClass(com.jaldishop.backend.store.infrastructure.persistence.entity.StoreCategoryEntity.class)
                .setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                .setProperty("hibernate.boot.allow_jdbc_metadata_access", "false");
        try (var factory = config.buildSessionFactory(); var session = factory.openSession()) {
            String hql = StoreJpaRepository.class.getMethod("searchStores", String.class, StoreStatus.class)
                    .getAnnotation(Query.class).value();
            var query = session.createQuery(hql, StoreEntity.class)
                    .setParameter("query", "Pastelería")
                    .setParameter("status", StoreStatus.ACTIVE);
            assertThat(query.getParameterValue("query")).isEqualTo("Pastelería");
        }
    }
}

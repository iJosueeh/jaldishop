package com.jaldishop.backend.ordering.infrastructure.persistence.repository;

import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.ordering.infrastructure.persistence.entity.OrderEntity;
import com.jaldishop.backend.ordering.infrastructure.persistence.entity.OrderItemEntity;
import com.jaldishop.backend.ordering.infrastructure.persistence.entity.OrderStatusHistoryEntity;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderSearchQueryTest {

    @Test
    @DisplayName("Hibernate compila y valida la consulta HQL de búsqueda y su consulta de conteo con dialecto PostgreSQL")
    void hibernateCompilesSearchQueryWithPostgresDialect() throws Exception {
        var config = new Configuration()
                .addAnnotatedClass(OrderEntity.class)
                .addAnnotatedClass(OrderItemEntity.class)
                .addAnnotatedClass(OrderStatusHistoryEntity.class)
                .setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                .setProperty("hibernate.boot.allow_jdbc_metadata_access", "false");

        try (var factory = config.buildSessionFactory(); var session = factory.openSession()) {
            var searchMethod = OrderJpaRepository.class.getMethod(
                    "searchOrders", UUID.class, OrderStatus.class, String.class, Pageable.class
            );
            Query queryAnnotation = searchMethod.getAnnotation(Query.class);
            String mainHql = queryAnnotation.value();
            String countHql = queryAnnotation.countQuery();

            // Verificaciones estructurales del contrato HQL
            assertThat(mainHql)
                    .contains("o.storeId = :storeId")
                    .contains("LOWER(o.orderNumber) LIKE :searchPattern ESCAPE '\\'")
                    .contains("LOWER(o.customerName) LIKE :searchPattern ESCAPE '\\'")
                    .contains("LOWER(o.customerPhone) LIKE :searchPattern ESCAPE '\\'")
                    .contains("ORDER BY o.confirmedAt DESC, o.id DESC")
                    .doesNotContain("JOIN FETCH")
                    .doesNotContain(":searchTerm IS NULL");

            assertThat(countHql)
                    .contains("SELECT COUNT(o) FROM OrderEntity o")
                    .contains("o.storeId = :storeId")
                    .contains("LOWER(o.orderNumber) LIKE :searchPattern ESCAPE '\\'")
                    .contains("LOWER(o.customerName) LIKE :searchPattern ESCAPE '\\'")
                    .contains("LOWER(o.customerPhone) LIKE :searchPattern ESCAPE '\\'")
                    .doesNotContain("ORDER BY");

            UUID storeId = UUID.randomUUID();
            String searchPattern = "%carlos%";

            // 1. Probar consulta principal con status presente
            var mainQueryWithStatus = session.createQuery(mainHql, OrderEntity.class)
                    .setParameter("storeId", storeId)
                    .setParameter("status", OrderStatus.CONFIRMED)
                    .setParameter("searchPattern", searchPattern);

            assertThat(mainQueryWithStatus.getParameterValue("storeId")).isEqualTo(storeId);
            assertThat(mainQueryWithStatus.getParameterValue("status")).isEqualTo(OrderStatus.CONFIRMED);
            assertThat(mainQueryWithStatus.getParameterValue("searchPattern")).isEqualTo(searchPattern);

            // 2. Probar consulta principal con status nulo (búsqueda global de la tienda)
            var mainQueryWithoutStatus = session.createQuery(mainHql, OrderEntity.class)
                    .setParameter("storeId", storeId)
                    .setParameter("status", null)
                    .setParameter("searchPattern", searchPattern);

            assertThat(mainQueryWithoutStatus.getParameterValue("status")).isNull();

            // 3. Probar consulta de conteo
            var countQuery = session.createQuery(countHql, Long.class)
                    .setParameter("storeId", storeId)
                    .setParameter("status", OrderStatus.CONFIRMED)
                    .setParameter("searchPattern", searchPattern);

            assertThat(countQuery.getParameterValue("searchPattern")).isEqualTo(searchPattern);
        }
    }
}

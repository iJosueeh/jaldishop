package com.jaldishop.backend.inventory.infrastructure.persistence;

import com.jaldishop.backend.inventory.infrastructure.persistence.entity.InventoryEntity;
import com.jaldishop.backend.inventory.infrastructure.persistence.repository.InventoryJpaRepository;
import com.jaldishop.backend.inventory.infrastructure.persistence.repository.StoreVariantInventoryProjection;
import com.jaldishop.backend.testutil.TestPostgresConfig;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class InventoryJpaRepositorySpringDataTest {

    @Autowired
    private InventoryJpaRepository inventoryJpaRepository;

    private static final UUID STORE_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e5");
    private static final UUID TRACKED_VARIANT_1 = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf302"); // seeded in V4

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        TestPostgresConfig config = TestPostgresConfig.fromEnvironment();
        registry.add("spring.datasource.url", config::url);
        registry.add("spring.datasource.username", config::username);
        registry.add("spring.datasource.password", config::password);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @BeforeAll
    static void migrateDb() {
        TestPostgresConfig config = TestPostgresConfig.fromEnvironment();
        Flyway flyway = Flyway.configure()
                .dataSource(config.url(), config.username(), config.password())
                .locations("classpath:db/migration")
                .validateOnMigrate(config.validateOnMigrate())
                .load();
        flyway.migrate();
    }

    @Test
    @DisplayName("Spring Data JPA: invoca findAllVariantInventoriesByStoreId y mapea proyecciones correctamente")
    void testFindAllVariantInventoriesByStoreId() {
        List<StoreVariantInventoryProjection> list = inventoryJpaRepository.findAllVariantInventoriesByStoreId(STORE_ID);

        assertThat(list).isNotEmpty();
        StoreVariantInventoryProjection item = list.stream()
                .filter(p -> TRACKED_VARIANT_1.equals(p.getVariantId()))
                .findFirst()
                .orElseThrow();

        assertThat(item.getProductName()).isNotBlank();
        assertThat(item.getPresentationName()).isNotBlank();
        assertThat(item.isTracksInventory()).isTrue();
        assertThat(item.getPhysicalQuantity()).isNotNull();
    }

    @Test
    @DisplayName("Spring Data JPA: invoca findVariantInventoryByStoreIdAndVariantId con alias nativos")
    void testFindVariantInventoryByStoreIdAndVariantId() {
        Optional<StoreVariantInventoryProjection> opt = inventoryJpaRepository
                .findVariantInventoryByStoreIdAndVariantId(STORE_ID, TRACKED_VARIANT_1);

        assertThat(opt).isPresent();
        StoreVariantInventoryProjection proj = opt.get();
        assertThat(proj.getVariantId()).isEqualTo(TRACKED_VARIANT_1);
        assertThat(proj.getProductId()).isNotNull();
        assertThat(proj.getSku()).isNotBlank();
        assertThat(proj.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("Spring Data JPA: verifica upsertStock con clearAutomatically y lectura de proyección fresca")
    void testUpsertStockAndRefreshPersistenceContext() {
        int rows = inventoryJpaRepository.upsertStock(TRACKED_VARIANT_1, 99, Instant.now());
        assertThat(rows).isEqualTo(1);

        Optional<StoreVariantInventoryProjection> opt = inventoryJpaRepository
                .findVariantInventoryByStoreIdAndVariantId(STORE_ID, TRACKED_VARIANT_1);

        assertThat(opt).isPresent();
        assertThat(opt.get().getPhysicalQuantity()).isEqualTo(99);
    }
}

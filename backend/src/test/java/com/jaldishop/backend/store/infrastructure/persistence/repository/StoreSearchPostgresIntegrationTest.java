package com.jaldishop.backend.store.infrastructure.persistence.repository;

import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
import com.jaldishop.backend.shared.util.MethodValidationTestSupport;
import com.jaldishop.backend.store.application.service.SearchPublicStoresService;
import com.jaldishop.backend.store.domain.enums.StoreStatus;
import com.jaldishop.backend.store.infrastructure.persistence.adapter.StoreRepositoryAdapter;
import com.jaldishop.backend.store.infrastructure.persistence.entity.StoreEntity;
import com.jaldishop.backend.store.infrastructure.persistence.mapper.StorePersistenceMapper;
import com.jaldishop.backend.store.web.controller.StoreController;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.net.ServerSocket;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Owns a fresh cluster. Never reads .env, application profiles or external DB URLs. */
@EnabledIfEnvironmentVariable(named = "STORE_SEARCH_TEST_PG_BIN", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StoreSearchPostgresIntegrationTest {
    @TempDir static Path temporary;
    private Process postgres;
    private AnnotationConfigApplicationContext context;
    private JdbcTemplate jdbc;
    private MockMvc mvc;

    @BeforeAll
    void startDisposablePostgres() throws Exception {
        Path bin = Path.of(System.getenv("STORE_SEARCH_TEST_PG_BIN"));
        Path data = temporary.resolve("data");
        Path initLog = temporary.resolve("init.log");
        Process init = new ProcessBuilder(bin.resolve("initdb").toString(), "-D", data.toString(),
                "-U", "store_search_test", "-A", "trust", "--no-locale", "--encoding=UTF8")
                .redirectErrorStream(true).redirectOutput(initLog.toFile()).start();
        assertThat(init.waitFor(30, TimeUnit.SECONDS)).isTrue();
        assertThat(init.exitValue()).withFailMessage(Files.readString(initLog)).isZero();
        int port;
        try (ServerSocket available = new ServerSocket(0, 0, java.net.InetAddress.getLoopbackAddress())) {
            port = available.getLocalPort();
        }
        Path serverLog = temporary.resolve("postgres.log");
        postgres = new ProcessBuilder(bin.resolve("postgres").toString(), "-D", data.toString(),
                "-h", "127.0.0.1", "-p", Integer.toString(port), "-k", temporary.toString())
                .redirectErrorStream(true).redirectOutput(serverLog.toFile()).start();
        var ds = new DriverManagerDataSource("jdbc:postgresql://127.0.0.1:" + port + "/postgres", "store_search_test", "");
        jdbc = new JdbcTemplate(ds);
        boolean ready = false;
        for (int attempt = 0; attempt < 100 && postgres.isAlive(); attempt++) {
            try {
                jdbc.queryForObject("SELECT 1", Integer.class);
                ready = true;
                break;
            } catch (org.springframework.dao.DataAccessException unavailable) {
                TimeUnit.MILLISECONDS.sleep(100);
            }
        }
        assertThat(ready).withFailMessage(Files.readString(serverLog)).isTrue();
        context = new AnnotationConfigApplicationContext();
        context.registerBean(DataSource.class, () -> ds);
        context.register(Persistence.class, SearchPublicStoresService.class);
        context.registerBean(StoreRepositoryAdapter.class, () -> new StoreRepositoryAdapter(
                context.getBean(StoreJpaRepository.class), null, null, new StorePersistenceMapper()));
        context.refresh();
        new ResourceDatabasePopulator(new ClassPathResource(
                "db/migration/V26__enable_unaccent_for_store_search.sql")).execute(ds);
        var controller = new StoreController(null, null, null, null,
                context.getBean(SearchPublicStoresService.class), null);
        mvc = MockMvcBuilders.standaloneSetup(MethodValidationTestSupport.withParameterValidation(controller))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        store("Pastelería Delicia", "pasteleria-delicia", "ACTIVE", 1);
        store("Pastelería Dulce Deleite", "dulce-deleite", "ACTIVE", 2);
        store("Panadería Norte", "pan-del-norte", "ACTIVE", 3);
        store("PANADERÍA Sur", "horno-sur", "ACTIVE", 4);
        store("Oferta 50% real", "oferta-porcentaje", "ACTIVE", 5);
        store("Tienda_A", "guion-bajo", "ACTIVE", 6);
        store("Ruta \\ centro", "ruta-central", "ACTIVE", 7);
        store("Pastelería Oculta", "pasteleria-inactiva", "INACTIVE", 8);
        store("Pastelería Suspendida", "pasteleria-suspendida", "SUSPENDED", 9);
        store("Pastelería Cerrada", "pasteleria-cerrada", "CLOSED", 10);
    }

    @AfterAll
    void stopDisposablePostgres() throws Exception {
        if (context != null) context.close();
        if (postgres != null) {
            postgres.destroy();
            if (!postgres.waitFor(10, TimeUnit.SECONDS)) postgres.destroyForcibly().waitFor();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"pasteleria", "Pastelería", "PASTELERIA", "PASTELERÍA", "  pasteleria  ", "Pasteleri\u0301a"})
    void matchesBothBakeriesAndExcludesHiddenStores(String q) throws Exception {
        matches(q, "pasteleria-delicia", "dulce-deleite");
    }

    @ParameterizedTest
    @ValueSource(strings = {"panaderia", "PANADERÍA"})
    void matchesBreadShops(String q) throws Exception {
        matches(q, "pan-del-norte", "horno-sur");
    }

    @Test
    void matchesNameAndSlugOnly() throws Exception {
        matches("dulce", "dulce-deleite");
        matches("PAN-DEL-NORTE", "pan-del-norte");
        matches("busquedasecreta"); // Only present in description.
    }

    @Test
    void treatsWildcardsAndEscapeLiterally() throws Exception {
        matches("%", "oferta-porcentaje");
        matches("_", "guion-bajo");
        matches("\\", "ruta-central");
        matches("50%", "oferta-porcentaje");
        matches("%_\\");
        matches("' OR 1=1 --");
    }

    @Test
    void returnsEmptyArrayWhenUnmatched() throws Exception {
        matches("zzzz-sin-coincidencias");
    }

    @Test
    void preservesBlankAndMissingQueryAndFeaturedBehavior() throws Exception {
        for (String q : List.of("", "   ")) {
            mvc.perform(get("/api/v1/stores/search").param("q", q).param("limit", "50"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(7));
        }
        mvc.perform(get("/api/v1/stores/search"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(5));
        mvc.perform(get("/api/v1/stores/featured"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    void enforcesLimitsAndPreservesNewestFirstOrder() throws Exception {
        for (String limit : List.of("0", "51")) {
            mvc.perform(get("/api/v1/stores/search").param("q", "pasteleria").param("limit", limit))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
        mvc.perform(get("/api/v1/stores/search").param("q", "pasteleria").param("limit", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("dulce-deleite"));
    }

    @Test
    void preservesAdministrativeStatusFilter() {
        var repository = context.getBean(StoreJpaRepository.class);
        assertThat(repository.searchStores("pasteleria", StoreStatus.INACTIVE))
                .extracting(StoreEntity::getSlug).containsExactly("pasteleria-inactiva");
        assertThat(repository.searchStores("pasteleria", null)).hasSize(5);
    }

    private void matches(String q, String... slugs) throws Exception {
        mvc.perform(get("/api/v1/stores/search").param("q", q).param("limit", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].slug", containsInAnyOrder(slugs)));
    }

    private void store(String name, String slug, String status, int order) {
        jdbc.update("""
                INSERT INTO stores (id, owner_user_id, name, slug, description, pickup_enabled,
                    delivery_enabled, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'busquedasecreta', true, false, ?,
                    TIMESTAMPTZ '2026-01-01 00:00:00+00' + ? * INTERVAL '1 second', now())
                """, UUID.randomUUID(), UUID.randomUUID(), name, slug, status, order);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = StoreJpaRepository.class,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = StoreJpaRepository.class),
            excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                    classes = {StoreCategoryJpaRepository.class, StoreCustomerJpaRepository.class}))
    static class Persistence {
        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource ds) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(ds);
            factory.setPackagesToScan(StoreEntity.class.getPackageName());
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
            return factory;
        }

        @Bean
        JpaTransactionManager transactionManager(jakarta.persistence.EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }
    }
}

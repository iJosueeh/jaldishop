package com.jaldishop.backend.store.infrastructure.persistence.repository;

import com.jaldishop.backend.identity.infrastructure.persistence.entity.UserEntity;
import com.jaldishop.backend.store.application.service.GetStoreCustomersService;
import com.jaldishop.backend.store.infrastructure.persistence.adapter.StoreCustomerRepositoryAdapter;
import com.jaldishop.backend.testutil.TestPostgresConfig;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StoreCustomerPostgresIntegrationTest {

    private final String schema = "it_store_cust_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private AnnotationConfigApplicationContext context;
    private JdbcTemplate jdbc;
    private GetStoreCustomersService service;
    private StoreCustomerJpaRepository repository;
    private final UUID storeA = UUID.randomUUID(), storeB = UUID.randomUUID();
    private UUID customer, noOrders;

    @BeforeAll
    void setupDatabase() {
        TestPostgresConfig config = TestPostgresConfig.fromEnvironment();
        DriverManagerDataSource adminDs = new DriverManagerDataSource(config.url(), config.username(), config.password());
        new JdbcTemplate(adminDs).execute("CREATE SCHEMA " + schema);

        String schemaUrl = config.url() + "?currentSchema=" + schema + ",public";
        DriverManagerDataSource ds = new DriverManagerDataSource(schemaUrl, config.username(), config.password());
        jdbc = new JdbcTemplate(ds);

        jdbc.execute("CREATE TABLE users (id UUID PRIMARY KEY, first_name TEXT, last_name TEXT, email TEXT, phone TEXT, created_at TIMESTAMPTZ NOT NULL)");
        jdbc.execute("CREATE TABLE user_roles (user_id UUID, role_id SMALLINT, store_id UUID)");
        jdbc.execute("CREATE TABLE orders (id UUID PRIMARY KEY, user_id UUID, store_id UUID, status TEXT, total_amount NUMERIC(12,2), confirmed_at TIMESTAMPTZ NOT NULL)");

        context = new AnnotationConfigApplicationContext();
        context.registerBean(DataSource.class, () -> ds);
        context.register(Persistence.class, StoreCustomerRepositoryAdapter.class, GetStoreCustomersService.class);
        context.refresh();

        service = context.getBean(GetStoreCustomersService.class);
        repository = context.getBean(StoreCustomerJpaRepository.class);

        customer = user("Carlos", "Garcia", "carlos@example.test", "987654321", "2026-09-01T05:00:00.123456-05:00");
        noOrders = user("Ana", "Lopez", "ana@example.test", null, "2026-09-02T10:00:00.123456Z");
        UUID foreign = user("Foreign", "Only", "foreign@example.test", null, "2026-09-03T10:00:00Z");
        UUID unassigned = user("Unassigned", "Order", "unassigned@example.test", null, "2026-09-04T10:00:00Z");

        associate(customer, storeA, 1); associate(customer, storeB, 1);
        associate(noOrders, storeA, 1); associate(foreign, storeB, 1);
        associate(foreign, storeA, 2); associate(foreign, null, 1);

        order(customer, storeA, "COMPLETED", "10.25", "2026-09-10T10:00:00Z");
        order(customer, storeA, "CONFIRMED", "20.50", "2026-09-11T10:00:00Z");
        order(customer, storeA, "CANCELLED", "99.99", "2026-09-15T10:30:00.654321-05:00");
        order(customer, storeB, "COMPLETED", "900.00", "2026-10-01T10:00:00Z");
        order(unassigned, storeA, "COMPLETED", "800.00", "2026-10-01T10:00:00Z");
    }

    @AfterAll
    void tearDown() {
        if (context != null) {
            context.close();
        }
        try {
            TestPostgresConfig config = TestPostgresConfig.fromEnvironment();
            DriverManagerDataSource adminDs = new DriverManagerDataSource(config.url(), config.username(), config.password());
            new JdbcTemplate(adminDs).execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        } catch (Exception ignored) {
        }
    }

    @Test
    void materializesTimestamptzAsInstantThroughHibernateAndSpringData() {
        var factory = context.getBean(jakarta.persistence.EntityManagerFactory.class);
        try (var em = factory.createEntityManager()) {
            Object[] raw = (Object[]) em.createNativeQuery("SELECT created_at, (SELECT MAX(confirmed_at) FROM orders WHERE store_id = :store AND user_id = :id) FROM users WHERE id = :id")
                    .setParameter("store", storeA).setParameter("id", customer).getSingleResult();
            assertThat(raw[0]).isInstanceOf(Instant.class).isEqualTo(Instant.parse("2026-09-01T10:00:00.123456Z"));
            assertThat(raw[1]).isInstanceOf(Instant.class).isEqualTo(Instant.parse("2026-09-15T15:30:00.654321Z"));
        }
        var projected = repository.findCustomersByStoreId(storeA, "Carlos").getFirst();
        assertThat(projected.getCustomerSince()).isEqualTo(Instant.parse("2026-09-01T10:00:00.123456Z"));
        assertThat(projected.getLastOrderAt()).isEqualTo(Instant.parse("2026-09-15T15:30:00.654321Z"));
    }

    @Test
    void preservesAssociationScopeCountsAmountsNullsAndOrder() {
        var list = service.execute(storeA, null);
        assertThat(list).extracting(c -> c.userId()).containsExactly(noOrders, customer);
        assertThat(list.getFirst().customerSince()).isEqualTo(Instant.parse("2026-09-02T10:00:00.123456Z"));
        assertThat(list.getFirst().lastOrderAt()).isNull();
        assertThat(list.getFirst().ordersCount()).isZero();
        assertThat(list.getFirst().totalSpentAmount()).isEqualByComparingTo("0");
        assertThat(list.get(1).ordersCount()).isEqualTo(3);
        assertThat(list.get(1).totalSpentAmount()).isEqualByComparingTo("30.75");
        assertThat(list.get(1).lastOrderAt()).isEqualTo(Instant.parse("2026-09-15T15:30:00.654321Z"));
        var other = service.execute(storeB, "Carlos").getFirst();
        assertThat(other.ordersCount()).isEqualTo(1);
        assertThat(other.totalSpentAmount()).isEqualByComparingTo("900.00");
        assertThat(service.execute(UUID.randomUUID(), null)).isEmpty();
    }

    @Test
    void preservesBlankAndFilteredSearches() {
        for (String query : List.of("", "   ")) assertThat(service.execute(storeA, query)).hasSize(2);
        for (String query : List.of("  CARLOS  ", "garcia", "CARLOS@EXAMPLE.TEST", "987654321")) {
            assertThat(service.execute(storeA, query)).extracting(c -> c.userId()).containsExactly(customer);
        }
        assertThat(service.execute(storeA, "Foreign")).isEmpty();
        assertThat(service.execute(storeA, "no-match")).isEmpty();
        assertThat(service.execute(storeA, "' OR 1=1 --")).isEmpty();
    }

    private UUID user(String first, String last, String email, String phone, String since) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users VALUES (?, ?, ?, ?, ?, CAST(? AS TIMESTAMPTZ))", id, first, last, email, phone, since);
        return id;
    }
    private void associate(UUID user, UUID store, int role) {
        jdbc.update("INSERT INTO user_roles VALUES (?, ?, ?)", user, role, store);
    }
    private void order(UUID user, UUID store, String status, String amount, String at) {
        jdbc.update("INSERT INTO orders VALUES (?, ?, ?, ?, ?, CAST(? AS TIMESTAMPTZ))",
                UUID.randomUUID(), user, store, status, new java.math.BigDecimal(amount), at);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = StoreCustomerJpaRepository.class,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = StoreCustomerJpaRepository.class),
            excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                    classes = {StoreCategoryJpaRepository.class, StoreJpaRepository.class}))
    static class Persistence {
        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource ds) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(ds);
            factory.setPackagesToScan(UserEntity.class.getPackageName());
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "none"));
            return factory;
        }
        @Bean
        JpaTransactionManager transactionManager(jakarta.persistence.EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }
    }
}

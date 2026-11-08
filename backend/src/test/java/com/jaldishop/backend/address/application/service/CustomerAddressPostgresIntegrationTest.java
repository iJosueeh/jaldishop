package com.jaldishop.backend.address.application.service;

import com.jaldishop.backend.address.infrastructure.persistence.adapter.JpaCustomerAddressRepositoryAdapter;
import com.jaldishop.backend.address.infrastructure.persistence.entity.CustomerAddressEntity;
import com.jaldishop.backend.address.infrastructure.persistence.mapper.CustomerAddressPersistenceMapper;
import com.jaldishop.backend.address.infrastructure.persistence.repository.CustomerAddressJpaRepository;
import com.jaldishop.backend.address.infrastructure.ubigeo.UbigeoCatalogService;
import com.jaldishop.backend.address.web.dto.CreateCustomerAddressRequest;
import com.jaldishop.backend.address.web.dto.UpdateCustomerAddressRequest;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.net.ServerSocket;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

/** Owns a fresh cluster, never reads application profiles, .env or DB_URL. */
@EnabledIfEnvironmentVariable(named = "ADDRESS_TEST_PG_BIN", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CustomerAddressPostgresIntegrationTest {
    @TempDir static Path temporary;
    private Process postgres;
    private AnnotationConfigApplicationContext context;
    private JdbcTemplate jdbc;
    private CreateCustomerAddressService create;
    private UpdateCustomerAddressService update;
    private GetCustomerAddressesService get;

    @BeforeAll
    void startDisposablePostgres() throws Exception {
        Path bin = Path.of(System.getenv("ADDRESS_TEST_PG_BIN"));
        Path data = temporary.resolve("data");
        Path initLog = temporary.resolve("init.log");
        Process init = new ProcessBuilder(bin.resolve("initdb").toString(), "-D", data.toString(),
                "-U", "address_test", "-A", "trust", "--no-locale", "--encoding=UTF8")
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
        var ds = new DriverManagerDataSource("jdbc:postgresql://127.0.0.1:" + port + "/postgres", "address_test", "");
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
        // V15 only requires the user's primary key. No unrelated modules are booted.
        jdbc.execute("CREATE TABLE users (id UUID PRIMARY KEY)");
        new ResourceDatabasePopulator(new ClassPathResource(
                "db/migration/V15__create_customer_addresses_table.sql")).execute(ds);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(DataSource.class, () -> ds);
        context.register(Persistence.class, CustomerAddressPersistenceMapper.class,
                JpaCustomerAddressRepositoryAdapter.class, CreateCustomerAddressService.class,
                UpdateCustomerAddressService.class, GetCustomerAddressesService.class,
                UbigeoCatalogService.class);
        context.registerBean(ObjectMapper.class, () -> new ObjectMapper());
        context.refresh();
        create = context.getBean(CreateCustomerAddressService.class);
        update = context.getBean(UpdateCustomerAddressService.class);
        get = context.getBean(GetCustomerAddressesService.class);
    }

    @AfterAll
    void stopDisposablePostgres() throws Exception {
        if (context != null) context.close();
        if (postgres != null) {
            postgres.destroy();
            if (!postgres.waitFor(10, TimeUnit.SECONDS)) postgres.destroyForcibly().waitFor();
        }
    }

    @Test
    void shouldScopeIdentityByUserAndPreserveInteriorsAndUpdates() {
        UUID first = user();
        UUID second = user();
        var address = create.execute(first, request("Av. Larco 123 Dpto. 201"));
        assertThat(address.isDefault()).isTrue();
        assertDuplicate(() -> create.execute(first, request("Av. Larco 123 Dpto. 201")));
        assertDuplicate(() -> create.execute(first, request(" AV.  LARCO 123 DPTO. 201 ")));
        create.execute(second, request("Av. Larco 123 Dpto. 201"));
        var other = create.execute(first, request("Av. Larco 123 Dpto. 202"));
        assertDuplicate(() -> update.execute(first, other.id(), updateRequest("Av. Larco 123 Dpto. 201")));
        update.execute(first, address.id(), updateRequest(" AV.  LARCO 123 DPTO. 201 "));
        assertThat(get.execute(first).total()).isEqualTo(2);
        assertThat(get.execute(second).total()).isEqualTo(1);
        assertThat(get.execute(first).addresses().stream().filter(a -> a.isDefault()).count()).isEqualTo(1);
        assertThatThrownBy(() -> jdbc.update("UPDATE customer_addresses SET is_default = true WHERE id = ?", other.id()))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void shouldSerializeIdenticalConcurrentCreates() throws Exception {
        UUID user = user();
        var results = race(user, Collections.nCopies(8, "Av. Larco 123"));
        assertThat(results).containsExactlyInAnyOrder("CREATED", "ADDRESS_ALREADY_EXISTS", "ADDRESS_ALREADY_EXISTS",
                "ADDRESS_ALREADY_EXISTS", "ADDRESS_ALREADY_EXISTS", "ADDRESS_ALREADY_EXISTS",
                "ADDRESS_ALREADY_EXISTS", "ADDRESS_ALREADY_EXISTS");
        assertThat(get.execute(user).total()).isEqualTo(1);
    }

    @Test
    void shouldKeepFiveAddressLimitAndOneDefaultUnderConcurrency() throws Exception {
        UUID user = user();
        var streets = java.util.stream.IntStream.range(0, 8).mapToObj(i -> "Calle " + i).toList();
        var results = race(user, streets);
        assertThat(Collections.frequency(results, "CREATED")).isEqualTo(5);
        assertThat(Collections.frequency(results, "ADDRESS_LIMIT_EXCEEDED")).isEqualTo(3);
        var list = get.execute(user);
        assertThat(list.total()).isEqualTo(5);
        assertThat(list.addresses().stream().filter(a -> a.isDefault()).count()).isEqualTo(1);
    }

    private List<String> race(UUID user, List<String> streets) throws Exception {
        try (var pool = Executors.newFixedThreadPool(streets.size())) {
            var barrier = new CyclicBarrier(streets.size());
            List<Future<String>> futures = new ArrayList<>();
            for (String street : streets) futures.add(pool.submit(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                try {
                    create.execute(user, request(street));
                    return "CREATED";
                } catch (BusinessRuleException ex) {
                    return ex.getCode();
                }
            }));
            List<String> results = new ArrayList<>();
            for (var future : futures) results.add(future.get(30, TimeUnit.SECONDS));
            return results;
        }
    }

    private UUID user() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id) VALUES (?)", id);
        return id;
    }

    private CreateCustomerAddressRequest request(String street) {
        return new CreateCustomerAddressRequest("Casa", "Juan", "987654321", street,
                "Lima", "Lima", "Miraflores", "150122", null, null, null, true);
    }

    private UpdateCustomerAddressRequest updateRequest(String street) {
        return new UpdateCustomerAddressRequest("Alias nuevo", "Ana", "912345678", street,
                "Lima", "Lima", "Miraflores", "150122", "Portón verde", null, null, true);
    }

    private void assertDuplicate(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOfSatisfying(BusinessRuleException.class,
                ex -> assertThat(ex.getCode()).isEqualTo("ADDRESS_ALREADY_EXISTS"));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = CustomerAddressJpaRepository.class)
    static class Persistence {
        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource ds) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(ds);
            factory.setPackagesToScan(CustomerAddressEntity.class.getPackageName());
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

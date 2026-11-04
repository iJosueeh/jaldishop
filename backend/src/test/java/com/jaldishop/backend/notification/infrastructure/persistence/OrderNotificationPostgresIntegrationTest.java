package com.jaldishop.backend.notification.infrastructure.persistence;
import com.jaldishop.backend.notification.application.command.*;
import com.jaldishop.backend.notification.application.service.*;
import com.jaldishop.backend.notification.domain.model.*;
import com.jaldishop.backend.notification.domain.enums.*;
import com.jaldishop.backend.notification.domain.repository.*;
import com.jaldishop.backend.notification.domain.port.*;


import com.jaldishop.backend.notification.application.service.*;
import com.jaldishop.backend.notification.domain.model.Notification;
import com.jaldishop.backend.notification.domain.repository.NotificationRepository;
import com.jaldishop.backend.notification.domain.enums.NotificationStatus;
import com.jaldishop.backend.notification.domain.enums.NotificationType;
import com.jaldishop.backend.notification.infrastructure.listener.OrderNotificationEventListener;
import com.jaldishop.backend.notification.web.controller.NotificationController;
import com.jaldishop.backend.ordering.domain.event.OrderConfirmedDomainEvent;
import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import com.jaldishop.backend.shared.util.MethodValidationTestSupport;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
import com.jaldishop.backend.testutil.TestPostgresConfig;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@Import(OrderNotificationPostgresIntegrationTest.SafeTestDatabase.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("PostgreSQL Real Integration Tests: Notification N2 with Flyway V16")
class OrderNotificationPostgresIntegrationTest {

    private static final TestPostgresConfig database = TestPostgresConfig.fromEnvironment();

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private IsolatedOrderNotificationPersister isolatedPersister;

    @Autowired
    private OrderNotificationEventListener listener;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ListUserNotificationsService listUserNotificationsService;

    @Autowired
    private CountUnreadNotificationsService countUnreadNotificationsService;

    @Autowired
    private MarkNotificationAsReadService markNotificationAsReadService;

    private static final UUID merchantAId = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID storeAId = UUID.fromString("00000000-0000-0000-0000-0000000000a2");
    private static final UUID merchantBId = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private static final UUID storeBId = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

    private static final String STORE_A_SLUG = "tienda-a-n2-test";
    private static final String STORE_B_SLUG = "tienda-b-n2-test";
    private static final String EMAIL_A = "merchantA_n2@jaldishop.test";
    private static final String EMAIL_B = "merchantB_n2@jaldishop.test";

    private MockMvc mockMvcMerchantA;

    @BeforeAll
    static void initSchemaAndData() throws SQLException {
        // Ejecutar Flyway V1 a V16 contra jaldishop_test
        Flyway flyway = Flyway.configure()
                .dataSource(database.url(), database.username(), database.password())
                .locations("classpath:db/migration")
                .validateOnMigrate(database.validateOnMigrate())
                .load();
        flyway.migrate();

        // Limpiar de forma determinista cualquier dato remanente si una ejecución previa terminó abruptamente
        cleanTestData();

        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            // Insertar comerciantes
            insertTestUser(conn, merchantAId, EMAIL_A, "Comerciante A");
            insertTestUser(conn, merchantBId, EMAIL_B, "Comerciante B");

            // Insertar tiendas
            insertTestStore(conn, storeAId, merchantAId, "Tienda A Test N2", STORE_A_SLUG);
            insertTestStore(conn, storeBId, merchantBId, "Tienda B Test N2", STORE_B_SLUG);
        }
    }

    @AfterAll
    static void cleanUpTestData() throws SQLException {
        cleanTestData();
    }

    private static void cleanTestData() throws SQLException {
        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            conn.setAutoCommit(true);
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM notifications WHERE user_id IN (?, ?)")) {
                ps.setObject(1, merchantAId);
                ps.setObject(2, merchantBId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM stores WHERE id IN (?, ?)")) {
                ps.setObject(1, storeAId);
                ps.setObject(2, storeBId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM users WHERE id IN (?, ?)")) {
                ps.setObject(1, merchantAId);
                ps.setObject(2, merchantBId);
                ps.executeUpdate();
            }
        }
    }

    @BeforeEach
    void setUpMockMvc() {
        NotificationController controller = new NotificationController(
                listUserNotificationsService,
                countUnreadNotificationsService,
                markNotificationAsReadService
        );

        HandlerMethodArgumentResolver merchantAPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                        && parameter.getParameterType().isAssignableFrom(JwtPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return new JwtPrincipal(merchantAId, Set.of("MERCHANT"));
            }
        };

        mockMvcMerchantA = MockMvcBuilders.standaloneSetup(MethodValidationTestSupport.withParameterValidation(controller))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(merchantAPrincipalResolver)
                .build();
    }

    private static void insertTestUser(Connection conn, UUID id, String email, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO users (id, email, password_encoded, first_name, last_name, phone, status, created_at, updated_at)
                VALUES (?, ?, '$2a$10$dummyHashForTestingN2Only00000000000000000000000000000', 'Comerciante', 'Test', '999888777', 'ACTIVE', NOW(), NOW())
                ON CONFLICT (id) DO NOTHING;
                """)) {
            ps.setObject(1, id);
            ps.setString(2, email);
            ps.executeUpdate();
        }
    }

    private static void insertTestStore(Connection conn, UUID id, UUID ownerId, String name, String slug) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO stores (id, owner_user_id, name, slug, description, contact_phone, address,
                                    pickup_enabled, delivery_enabled, delivery_fee_amount, delivery_fee_currency,
                                    tax_rate, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'Tienda de prueba N2', '999888777', 'Av. Test 123',
                        true, false, 0.00, 'PEN', 0.18, 'ACTIVE', NOW(), NOW())
                ON CONFLICT (id) DO NOTHING;
                """)) {
            ps.setObject(1, id);
            ps.setObject(2, ownerId);
            ps.setString(3, name);
            ps.setString(4, slug);
            ps.executeUpdate();
        }
    }

    @Test
    @Order(1)
    @DisplayName("IT-01: Migración V16 debe crear columna deduplication_key y restricción uq_notifications_deduplication_key")
    void testFlywayV16MigrationAppliesCleanly() throws SQLException {
        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            // Verificar existencia de columna
            try (PreparedStatement ps = conn.prepareStatement("""
                    SELECT column_name, data_type, is_nullable
                    FROM information_schema.columns
                    WHERE table_name = 'notifications' AND column_name = 'deduplication_key';
                    """)) {
                try (ResultSet rs = ps.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("column_name")).isEqualTo("deduplication_key");
                    assertThat(rs.getString("is_nullable")).isEqualTo("YES");
                }
            }

            // Verificar existencia de restricción UNIQUE
            try (PreparedStatement ps = conn.prepareStatement("""
                    SELECT conname
                    FROM pg_constraint
                    WHERE conname = 'uq_notifications_deduplication_key';
                    """)) {
                try (ResultSet rs = ps.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("conname")).isEqualTo("uq_notifications_deduplication_key");
                }
            }
        }
    }

    @Test
    @Order(2)
    @DisplayName("IT-02: Filas históricas con deduplication_key = NULL y múltiples NULLs deben coexistir pacíficamente")
    void testHistoricalRowsCompatibility() throws SQLException {
        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            // Insertar 2 filas manuales con deduplication_key = NULL para el mismo usuario
            UUID notif1 = UUID.randomUUID();
            UUID notif2 = UUID.randomUUID();

            for (UUID notifId : List.of(notif1, notif2)) {
                try (PreparedStatement ps = conn.prepareStatement("""
                        INSERT INTO notifications (id, user_id, type, title, message, status, created_at, deduplication_key)
                        VALUES (?, ?, 'SYSTEM', 'Mensaje del sistema', 'Contenido informativo', 'UNREAD', NOW(), NULL);
                        """)) {
                    ps.setObject(1, notifId);
                    ps.setObject(2, merchantAId);
                    int inserted = ps.executeUpdate();
                    assertThat(inserted).isEqualTo(1);
                }
            }

            // Verificar que ambas filas existen con deduplication_key NULL
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM notifications WHERE deduplication_key IS NULL AND user_id = ?;")) {
                ps.setObject(1, merchantAId);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    assertThat(rs.getLong(1)).isGreaterThanOrEqualTo(2);
                }
            }
        }
    }

    @Test
    @Order(3)
    @DisplayName("IT-03: Confirmación de pedido en checkout debe generar 1 notificación persistente NEW_ORDER")
    void testSingleOrderGeneratesPersistentNotification() {
        UUID orderId = UUID.randomUUID();
        String orderNumber = "ORD-20261101-0003";

        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new OrderConfirmedDomainEvent(
                    UUID.randomUUID(),
                    "ORDER_CONFIRMED",
                    orderId,
                    orderNumber,
                    storeAId,
                    new BigDecimal("85.50"),
                    "PEN",
                    Instant.now()
            ));
        });

        List<Notification> notifications = notificationRepository.findByUserId(merchantAId, PageRequest.of(0, 10));
        Optional<Notification> match = notifications.stream()
                .filter(n -> ("NEW_ORDER:" + orderId).equals(n.getDeduplicationKey()))
                .findFirst();

        assertThat(match).isPresent();
        Notification notification = match.get();
        assertThat(notification.getType()).isEqualTo(NotificationType.NEW_ORDER);
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.UNREAD);
        assertThat(notification.getTitle()).isEqualTo("Nuevo pedido recibido: " + orderNumber);
        assertThat(notification.getMessage()).isEqualTo("Has recibido una nueva orden. Ingresa a la sección de pedidos para gestionarla.");
        assertThat(notification.getDeduplicationKey()).isEqualTo("NEW_ORDER:" + orderId);
    }

    @Test
    @Order(4)
    @DisplayName("IT-04: Evento duplicado secuencial debe ser idempotente y no duplicar registros en base de datos")
    void testSequentialDuplicateIsIdempotent() {
        UUID orderId = UUID.randomUUID();
        String orderNumber = "ORD-20261101-0004";

        OrderConfirmedDomainEvent event = new OrderConfirmedDomainEvent(
                UUID.randomUUID(),
                "ORDER_CONFIRMED",
                orderId,
                orderNumber,
                storeAId,
                new BigDecimal("120.00"),
                "PEN",
                Instant.now()
        );

        // Disparo 1
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.executeWithoutResult(status -> eventPublisher.publishEvent(event));

        // Disparo 2 (duplicado)
        assertDoesNotThrow(() -> {
            template.executeWithoutResult(status -> eventPublisher.publishEvent(event));
        });

        // Verificar que en base de datos existe exactamente 1 registro para esta deduplicationKey
        List<Notification> notifications = notificationRepository.findByUserId(merchantAId, PageRequest.of(0, 50));
        long count = notifications.stream()
                .filter(n -> ("NEW_ORDER:" + orderId).equals(n.getDeduplicationKey()))
                .count();

        assertThat(count).isEqualTo(1);
    }

    @Test
    @Order(5)
    @DisplayName("IT-05: Inserciones concurrentes con la misma clave deben resultar en exactamente 1 fila")
    void testConcurrentDuplicateInserts() throws InterruptedException {
        UUID orderId = UUID.randomUUID();
        String deduplicationKey = "NEW_ORDER:" + orderId;
        int threads = 5;

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threads);

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger uniqueViolations = new AtomicInteger(0);
        AtomicInteger otherErrors = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    CreateNotificationCommand cmd = new CreateNotificationCommand(
                            merchantAId,
                            NotificationType.NEW_ORDER,
                            "Nuevo pedido recibido: CONC-" + orderId,
                            "Has recibido una nueva orden. Ingresa a la sección de pedidos para gestionarla.",
                            deduplicationKey
                    );
                    isolatedPersister.persistIsolated(cmd);
                    successes.incrementAndGet();
                } catch (Throwable ex) {
                    if (NotificationDeduplicationExceptionClassifier.isDeduplicationViolation(ex)) {
                        uniqueViolations.incrementAndGet();
                    } else {
                        otherErrors.incrementAndGet();
                    }
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = finishLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successes.get()).isEqualTo(1);
        assertThat(uniqueViolations.get()).isEqualTo(threads - 1);
        assertThat(otherErrors.get()).isEqualTo(0);

        // Verificar persistencia real
        List<Notification> notifications = notificationRepository.findByUserId(merchantAId, PageRequest.of(0, 50));
        long count = notifications.stream()
                .filter(n -> deduplicationKey.equals(n.getDeduplicationKey()))
                .count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    @Order(6)
    @DisplayName("IT-06: Violación de una restricción distinta a UNIQUE (ej. FK inexistente) NO debe clasificarse como deduplicación")
    void testNonUniqueIntegrityViolationIsLoggedAsError() {
        UUID nonExistentUserId = UUID.randomUUID();
        CreateNotificationCommand cmd = new CreateNotificationCommand(
                nonExistentUserId,
                NotificationType.NEW_ORDER,
                "Título de prueba",
                "Mensaje de prueba",
                "NEW_ORDER:" + UUID.randomUUID()
        );

        try {
            isolatedPersister.persistIsolated(cmd);
            org.junit.jupiter.api.Assertions.fail("Se esperaba excepción por violación de FK");
        } catch (Throwable ex) {
            boolean isDedup = NotificationDeduplicationExceptionClassifier.isDeduplicationViolation(ex);
            assertThat(isDedup).isFalse();
        }
    }

    @Test
    @Order(7)
    @DisplayName("IT-07: Rollback en la transacción de checkout NUNCA debe ejecutar el listener ni generar notificación")
    void testCheckoutRollbackDoesNotTriggerNotification() {
        UUID orderId = UUID.randomUUID();
        String orderNumber = "ORD-ROLLBACK-" + orderId;

        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        TransactionStatus status = transactionManager.getTransaction(def);

        eventPublisher.publishEvent(new OrderConfirmedDomainEvent(
                UUID.randomUUID(),
                "ORDER_CONFIRMED",
                orderId,
                orderNumber,
                storeAId,
                new BigDecimal("50.00"),
                "PEN",
                Instant.now()
        ));

        // Forzar Rollback
        transactionManager.rollback(status);

        // Verificar que no se creó ninguna notificación
        List<Notification> notifications = notificationRepository.findByUserId(merchantAId, PageRequest.of(0, 50));
        boolean exists = notifications.stream()
                .anyMatch(n -> ("NEW_ORDER:" + orderId).equals(n.getDeduplicationKey()));

        assertThat(exists).isFalse();
    }

    @Test
    @Order(8)
    @DisplayName("IT-08: Aislamiento entre comerciantes: Tienda A genera para Comerciante A y Tienda B para Comerciante B")
    void testMerchantIsolationAcrossStores() {
        UUID orderA = UUID.randomUUID();
        UUID orderB = UUID.randomUUID();

        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new OrderConfirmedDomainEvent(
                    UUID.randomUUID(), "ORDER_CONFIRMED", orderA, "ORD-A", storeAId, BigDecimal.TEN, "PEN", Instant.now()
            ));
            eventPublisher.publishEvent(new OrderConfirmedDomainEvent(
                    UUID.randomUUID(), "ORDER_CONFIRMED", orderB, "ORD-B", storeBId, BigDecimal.TEN, "PEN", Instant.now()
            ));
        });

        List<Notification> notifsA = notificationRepository.findByUserId(merchantAId, PageRequest.of(0, 50));
        List<Notification> notifsB = notificationRepository.findByUserId(merchantBId, PageRequest.of(0, 50));

        assertThat(notifsA.stream().anyMatch(n -> ("NEW_ORDER:" + orderA).equals(n.getDeduplicationKey()))).isTrue();
        assertThat(notifsA.stream().noneMatch(n -> ("NEW_ORDER:" + orderB).equals(n.getDeduplicationKey()))).isTrue();

        assertThat(notifsB.stream().anyMatch(n -> ("NEW_ORDER:" + orderB).equals(n.getDeduplicationKey()))).isTrue();
        assertThat(notifsB.stream().noneMatch(n -> ("NEW_ORDER:" + orderA).equals(n.getDeduplicationKey()))).isTrue();
    }

    @Test
    @Order(9)
    @DisplayName("IT-09: Endpoints REST consumidos por Frontend N1 deben listar la nueva notificación y actualizar contador")
    void testN1EndpointsReadNewNotification() throws Exception {
        UUID orderId = UUID.randomUUID();
        String orderNumber = "ORD-REST-" + orderId;

        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new OrderConfirmedDomainEvent(
                    UUID.randomUUID(), "ORDER_CONFIRMED", orderId, orderNumber, storeAId, BigDecimal.TEN, "PEN", Instant.now()
            ));
        });

        // Obtener la notificación de la base de datos
        List<Notification> notifs = notificationRepository.findByUserId(merchantAId, PageRequest.of(0, 10));
        Notification target = notifs.stream()
                .filter(n -> ("NEW_ORDER:" + orderId).equals(n.getDeduplicationKey()))
                .findFirst()
                .orElseThrow();

        // 1. GET /api/v1/notifications/unread/count
        mockMvcMerchantA.perform(get("/api/v1/notifications/unread/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").isNumber())
                .andExpect(jsonPath("$.unreadCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        // 2. GET /api/v1/notifications?page=0&size=20
        mockMvcMerchantA.perform(get("/api/v1/notifications?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.id == '" + target.getId() + "')].title")
                        .value("Nuevo pedido recibido: " + orderNumber))
                .andExpect(jsonPath("$[?(@.id == '" + target.getId() + "')].status").value("UNREAD"));

        // 3. PATCH /api/v1/notifications/{id}/read
        mockMvcMerchantA.perform(patch("/api/v1/notifications/" + target.getId() + "/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(target.getId().toString()))
                .andExpect(jsonPath("$.status").value("READ"))
                .andExpect(jsonPath("$.readAt").isNotEmpty());
    }

    @Test
    @Order(10)
    @DisplayName("IT-10: Respuesta HTTP de confirmación de compra conserva 201 Created aunque el listener de N2 falle post-commit")
    void testCheckoutHttpResponsePreservedWhenNotificationListenerFailsPostCommit() throws Exception {
        UUID orderId = UUID.randomUUID();
        String orderNumber = "ORD-HTTP-FAIL-" + orderId;
        UUID nonExistentStoreId = UUID.randomUUID();

        TestCheckoutSimulatedController testController = new TestCheckoutSimulatedController(transactionManager, eventPublisher);
        MockMvc mockMvcCheckout = MockMvcBuilders.standaloneSetup(testController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        // Ejecutar POST HTTP en mockMvcCheckout hacia un store no resoluble (falla controlada post-commit)
        mockMvcCheckout.perform(post("/api/test/checkout/confirm")
                        .param("orderId", orderId.toString())
                        .param("orderNumber", orderNumber)
                        .param("storeId", nonExistentStoreId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.orderNumber").value(orderNumber));

        // Verificar que no se creó notificación fantasma ni errónea
        List<Notification> notifs = notificationRepository.findByUserId(merchantAId, PageRequest.of(0, 50));
        boolean exists = notifs.stream().anyMatch(n -> ("NEW_ORDER:" + orderId).equals(n.getDeduplicationKey()));
        assertThat(exists).isFalse();
    }

    @RestController
    @RequestMapping("/api/test/checkout")
    static class TestCheckoutSimulatedController {
        private final PlatformTransactionManager transactionManager;
        private final ApplicationEventPublisher eventPublisher;

        TestCheckoutSimulatedController(PlatformTransactionManager transactionManager, ApplicationEventPublisher eventPublisher) {
            this.transactionManager = transactionManager;
            this.eventPublisher = eventPublisher;
        }

        @PostMapping("/confirm")
        public ResponseEntity<Map<String, Object>> confirm(@RequestParam UUID orderId,
                                                           @RequestParam String orderNumber,
                                                           @RequestParam UUID storeId) {
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.executeWithoutResult(status -> {
                eventPublisher.publishEvent(new OrderConfirmedDomainEvent(
                        UUID.randomUUID(),
                        "ORDER_CONFIRMED",
                        orderId,
                        orderNumber,
                        storeId,
                        new BigDecimal("99.90"),
                        "PEN",
                        Instant.now()
                ));
            });
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "orderId", orderId.toString(),
                    "orderNumber", orderNumber,
                    "status", "CONFIRMED"
            ));
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SafeTestDatabase {

        @Bean
        DataSource dataSource() {
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setUrl(database.url());
            dataSource.setUsername(database.username());
            dataSource.setPassword(database.password());
            return dataSource;
        }
    }

}

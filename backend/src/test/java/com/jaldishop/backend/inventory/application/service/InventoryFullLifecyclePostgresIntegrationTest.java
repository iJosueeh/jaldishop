package com.jaldishop.backend.inventory.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaldishop.backend.capacity.domain.enums.CapacityReservationStatus;
import com.jaldishop.backend.capacity.domain.model.CapacityReservation;
import com.jaldishop.backend.capacity.domain.repository.CapacityReservationRepository;
import com.jaldishop.backend.catalog.application.command.CreateProductCommand;
import com.jaldishop.backend.catalog.application.command.CreateProductVariantCommand;
import com.jaldishop.backend.catalog.application.service.CreateProductService;
import com.jaldishop.backend.catalog.application.service.CreateProductVariantService;
import com.jaldishop.backend.catalog.domain.model.Product;
import com.jaldishop.backend.catalog.domain.model.ProductVariant;
import com.jaldishop.backend.checkout.application.command.ConfirmPurchaseCommand;
import com.jaldishop.backend.checkout.application.result.OrderConfirmation;
import com.jaldishop.backend.checkout.application.service.ConfirmPurchaseService;
import com.jaldishop.backend.checkout.domain.model.CheckoutItemSnapshot;
import com.jaldishop.backend.identity.infrastructure.security.JwtService;
import com.jaldishop.backend.inventory.domain.enums.InventoryReservationStatus;
import com.jaldishop.backend.inventory.domain.model.InventoryReservation;
import com.jaldishop.backend.inventory.domain.repository.InventoryReservationRepository;
import com.jaldishop.backend.inventory.infrastructure.persistence.repository.InventoryJpaRepository;
import com.jaldishop.backend.inventory.web.dto.InventoryResponse;
import com.jaldishop.backend.ordering.application.service.CancelCustomerOrderService;
import com.jaldishop.backend.ordering.application.service.ChangeOrderStatusService;
import com.jaldishop.backend.ordering.application.service.OrderCancellationCompensator;
import com.jaldishop.backend.ordering.domain.enums.OrderStatus;
import com.jaldishop.backend.ordering.domain.model.Order;
import com.jaldishop.backend.ordering.domain.repository.OrderRepository;
import com.jaldishop.backend.ordering.web.dto.CustomerOrderDetailResponse;
import com.jaldishop.backend.payment.domain.enums.FulfillmentType;
import com.jaldishop.backend.payment.domain.enums.PaymentStatus;
import com.jaldishop.backend.payment.domain.enums.RefundStatus;
import com.jaldishop.backend.payment.domain.model.Payment;
import com.jaldishop.backend.payment.domain.repository.PaymentRepository;
import com.jaldishop.backend.realtime.application.service.RealtimeNotificationPublisher;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.testutil.TestPostgresConfig;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fase 4B: Pruebas de integración del ciclo de vida completo de inventario sobre PostgreSQL desechable.
 *
 * <p>Verifica:
 * 1. Compra confirmada (Creación -> Stock -> Hold -> Confirmación con pago controlado -> Deducción física -> COMMITTED -> Consulta Merchant).
 * 2. Cancelación y restitución (Pedido CONFIRMED -> Cancelación -> Restitución de stock -> Transición RELEASED -> No doble restitución).
 * 3. Cancelación en preparación (Avanzar a IN_PREPARATION -> Cancelación permitida -> Stock físico preservado sin restitución según RN-ORD-13).
 * 4. Concurrencia real (2 intentos compitiendo por 1 unidad física -> 1 éxito, 1 fallo INSUFFICIENT_STOCK -> Sin sobreventa en PostgreSQL).
 * 5. Expiración de reservas (Holds vencidos no reducen disponibilidad en consultas dinámicas -> Nuevo cliente puede reservar -> Transición EXPIRED persistida).
 * 6. Invariantes del sistema (Idempotencia, no sobreventa, no doble descuento, no doble restitución, aislamiento multitienda, conservación PAYMENT_PROTECTED).
 * 7. Verificación HTTP representativa (Endpoints REST Merchant /api/v1/inventory con JWT real contra servidor local y PostgreSQL real).</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "server.address=127.0.0.1",
                "spring.jpa.hibernate.ddl-auto=none"
        })
@Import(InventoryFullLifecyclePostgresIntegrationTest.SafeTestConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InventoryFullLifecyclePostgresIntegrationTest {

    private static final TestPostgresConfig database = TestPostgresConfig.fromEnvironment();

    // Tiendas y entidades sembradas en V4
    private static final UUID STORE_A_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e5");
    private static final UUID STORE_B_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e6");
    private static final UUID CATEGORY_A_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf101");
    private static final UUID MERCHANT_A_USER_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e1");
    private static final UUID MERCHANT_B_USER_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9f1");
    private static final UUID CUSTOMER_USER_ID = UUID.fromString("80acf0ba-5a79-4888-b00d-b5aef8fcf9e2");

    // Registro de identidades creadas para limpieza ordenada
    private static final Set<UUID> createdProductIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<UUID> createdVariantIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<UUID> createdCapResIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<UUID> createdPaymentIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<UUID> createdOrderIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @DynamicPropertySource
    static void configureTestDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", database::url);
        registry.add("spring.datasource.hikari.jdbc-url", database::url);
        registry.add("spring.datasource.username", database::username);
        registry.add("spring.datasource.hikari.username", database::username);
        registry.add("spring.datasource.password", database::password);
        registry.add("spring.datasource.hikari.password", database::password);
        registry.add("spring.flyway.url", database::url);
        registry.add("spring.flyway.user", database::username);
        registry.add("spring.flyway.password", database::password);
        registry.add("spring.flyway.validate-on-migrate", () -> "false");
    }

    @BeforeAll
    static void initSchema() {
        Flyway.configure()
                .dataSource(database.url(), database.username(), database.password())
                .locations("classpath:db/migration")
                .validateOnMigrate(false)
                .load()
                .migrate();
    }

    @LocalServerPort
    private int port;

    // Servicios del dominio
    @Autowired
    private CreateProductService createProductService;
    @Autowired
    private CreateProductVariantService createProductVariantService;
    @Autowired
    private UpdateInventoryStockService updateInventoryStockService;
    @Autowired
    private ReserveInventoryService reserveInventoryService;
    @Autowired
    private CommitInventoryReservationService commitInventoryReservationService;
    @Autowired
    private ConfirmPurchaseService confirmPurchaseService;
    @Autowired
    private CancelCustomerOrderService cancelCustomerOrderService;
    @Autowired
    private ChangeOrderStatusService changeOrderStatusService;
    @Autowired
    private RestockInventoryService restockInventoryService;
    @Autowired
    private GetVariantInventoryService getVariantInventoryService;
    @Autowired
    private GetStoreInventoryService getStoreInventoryService;
    @Autowired
    private OrderCancellationCompensator orderCancellationCompensator;

    // Repositorios
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private CapacityReservationRepository capacityReservationRepository;
    @Autowired
    private InventoryReservationRepository inventoryReservationRepository;
    @Autowired
    private InventoryJpaRepository inventoryJpaRepository;
    @Autowired
    private JwtService jwtService;

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    // Estado compartido entre Escenario 1 y 2
    private static UUID scenario1VariantId;
    private static UUID scenario1OrderId;
    private static String scenario1OrderNumber;
    private static UUID scenario1CapResId;
    private static UUID scenario1PaymentId;

    @AfterAll
    static void cleanUpTestData() throws SQLException {
        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            conn.setAutoCommit(true);

            // 1. Eliminar historial e ítems de pedidos creados
            for (UUID orderId : createdOrderIds) {
                try (var ps = conn.prepareStatement("DELETE FROM order_status_history WHERE order_id = ?")) {
                    ps.setObject(1, orderId);
                    ps.executeUpdate();
                }
                try (var ps = conn.prepareStatement("DELETE FROM order_items WHERE order_id = ?")) {
                    ps.setObject(1, orderId);
                    ps.executeUpdate();
                }
                try (var ps = conn.prepareStatement("DELETE FROM orders WHERE id = ?")) {
                    ps.setObject(1, orderId);
                    ps.executeUpdate();
                }
            }

            // 2. Eliminar pagos creados
            for (UUID paymentId : createdPaymentIds) {
                try (var ps = conn.prepareStatement("DELETE FROM payment_refund_execution_attempts WHERE refund_intent_id IN (SELECT id FROM payment_refund_intents WHERE payment_id = ?)")) {
                    ps.setObject(1, paymentId);
                    ps.executeUpdate();
                }
                try (var ps = conn.prepareStatement("DELETE FROM payment_refund_intents WHERE payment_id = ?")) {
                    ps.setObject(1, paymentId);
                    ps.executeUpdate();
                }
                try (var ps = conn.prepareStatement("DELETE FROM payment_attempts WHERE payment_id = ?")) {
                    ps.setObject(1, paymentId);
                    ps.executeUpdate();
                }
                try (var ps = conn.prepareStatement("DELETE FROM payments WHERE id = ?")) {
                    ps.setObject(1, paymentId);
                    ps.executeUpdate();
                }
            }

            // 3. Eliminar reservas de inventario y capacidad
            for (UUID capResId : createdCapResIds) {
                try (var ps = conn.prepareStatement("DELETE FROM inventory_reservations WHERE capacity_reservation_id = ?")) {
                    ps.setObject(1, capResId);
                    ps.executeUpdate();
                }
                try (var ps = conn.prepareStatement("DELETE FROM capacity_reservations WHERE id = ?")) {
                    ps.setObject(1, capResId);
                    ps.executeUpdate();
                }
            }

            // 4. Eliminar inventarios y variantes
            for (UUID variantId : createdVariantIds) {
                try (var ps = conn.prepareStatement("DELETE FROM inventory_reservations WHERE variant_id = ?")) {
                    ps.setObject(1, variantId);
                    ps.executeUpdate();
                }
                try (var ps = conn.prepareStatement("DELETE FROM inventories WHERE variant_id = ?")) {
                    ps.setObject(1, variantId);
                    ps.executeUpdate();
                }
                try (var ps = conn.prepareStatement("DELETE FROM product_variants WHERE id = ?")) {
                    ps.setObject(1, variantId);
                    ps.executeUpdate();
                }
            }

            // 5. Eliminar productos creados
            for (UUID productId : createdProductIds) {
                try (var ps = conn.prepareStatement("DELETE FROM products WHERE id = ?")) {
                    ps.setObject(1, productId);
                    ps.executeUpdate();
                }
            }
        }
    }

    private CheckoutItemSnapshot createSnapshot(UUID variantId, UUID productId, String name, int quantity, BigDecimal unitPrice) {
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        return new CheckoutItemSnapshot(
                variantId,
                productId,
                name,
                "Presentación",
                "SKU-" + variantId.toString().substring(0, 8),
                null,
                quantity,
                unitPrice,
                "PEN",
                subtotal,
                true
        );
    }

    // =========================================================================
    // ESCENARIO 1: COMPRA CONFIRMADA
    // =========================================================================

    @Test
    @org.junit.jupiter.api.Order(1)
    @DisplayName("Escenario 1: Compra confirmada completa (Creación -> Stock 10 -> Reserva 3 -> Pago Aprobado -> Stock 7 COMMITTED -> Pedido CONFIRMED -> Consulta Merchant)")
    void scenario1_shouldCompleteFullPurchaseLifecycle() throws Exception {
        // 1. Crear producto y variante real
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        Product product = createProductService.execute(new CreateProductCommand(
                STORE_A_ID,
                CATEGORY_A_ID,
                "Torta Chocolate Ciclo " + uniqueSuffix,
                "torta-choco-ciclo-" + uniqueSuffix,
                "Torta para prueba de ciclo completo",
                "https://cdn.jaldishop.pe/test-torta.jpg"
        ));
        createdProductIds.add(product.getId());

        ProductVariant variant = createProductVariantService.execute(new CreateProductVariantCommand(
                STORE_A_ID,
                product.getId(),
                "Porción 8 Personas",
                "SKU-TORTA-" + uniqueSuffix,
                new BigDecimal("60.00"),
                "PEN",
                true,
                Collections.emptyList()
        ));
        scenario1VariantId = variant.getId();
        createdVariantIds.add(scenario1VariantId);

        // 2. Configurar stock físico en 10 mediante servicio real
        InventoryResponse stockConfigResponse = updateInventoryStockService.execute(STORE_A_ID, scenario1VariantId, 10);
        assertThat(stockConfigResponse.physicalQuantity()).isEqualTo(10);
        assertThat(stockConfigResponse.reservedQuantity()).isEqualTo(0);
        assertThat(stockConfigResponse.availableQuantity()).isEqualTo(10);

        // Comprobación directa en PostgreSQL
        assertThat(queryPhysicalQuantityInPostgres(scenario1VariantId)).isEqualTo(10);

        // 3. Reservar 3 unidades mediante el flujo real
        UUID capResId = UUID.randomUUID();
        createdCapResIds.add(capResId);
        scenario1CapResId = capResId;
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);

        seedCapacityReservationInPostgres(capResId, STORE_A_ID, CUSTOMER_USER_ID, "ACTIVE", expiresAt, null);

        CheckoutItemSnapshot itemSnapshot = createSnapshot(
                scenario1VariantId,
                product.getId(),
                "Torta Chocolate Ciclo " + uniqueSuffix,
                3,
                new BigDecimal("60.00")
        );

        List<InventoryReservation> reservations = reserveInventoryService.execute(
                STORE_A_ID,
                capResId,
                expiresAt,
                List.of(itemSnapshot)
        );
        assertThat(reservations).hasSize(1);
        assertThat(reservations.getFirst().getStatus()).isEqualTo(InventoryReservationStatus.ACTIVE);
        assertThat(reservations.getFirst().getQuantity()).isEqualTo(3);

        // Verificar estado de reserva (hold activo) en PostgreSQL: stock físico sigue en 10
        assertThat(queryPhysicalQuantityInPostgres(scenario1VariantId)).isEqualTo(10);
        assertThat(queryInventoryReservationStatusInPostgres(capResId)).isEqualTo("ACTIVE");

        // Consultar disponibilidad Merchant durante el hold activo
        InventoryResponse holdStateMerchant = getVariantInventoryService.execute(STORE_A_ID, scenario1VariantId);
        assertThat(holdStateMerchant.physicalQuantity()).isEqualTo(10);
        assertThat(holdStateMerchant.reservedQuantity()).isEqualTo(3);
        assertThat(holdStateMerchant.availableQuantity()).isEqualTo(7);

        // 4. Confirmar la compra con pago controlado
        // 4a. Transicionar reserva de capacidad a PAYMENT_PROTECTED (regla BE-20)
        Instant protectionExpires = Instant.now().plus(10, ChronoUnit.MINUTES);
        updateCapacityReservationStatusInPostgres(capResId, "PAYMENT_PROTECTED", protectionExpires);

        // 4b. Crear pago APPROVED en PostgreSQL con el snapshot serializado
        UUID paymentId = UUID.randomUUID();
        scenario1PaymentId = paymentId;
        createdPaymentIds.add(paymentId);
        String snapshotJson = jsonMapper.writeValueAsString(List.of(itemSnapshot));

        seedPaymentInPostgres(
                paymentId,
                capResId,
                "APPROVED",
                new BigDecimal("180.00"),
                snapshotJson
        );

        // 4c. Orquestador real de confirmación de compra
        OrderConfirmation confirmation = confirmPurchaseService.execute(new ConfirmPurchaseCommand(paymentId));
        assertThat(confirmation).isNotNull();
        scenario1OrderId = confirmation.orderId();
        scenario1OrderNumber = confirmation.orderNumber();
        createdOrderIds.add(scenario1OrderId);

        // 5. Verificar stock físico 7 en PostgreSQL
        assertThat(queryPhysicalQuantityInPostgres(scenario1VariantId)).isEqualTo(7);

        // 6. Verificar reserva de inventario y capacidad COMMITTED en PostgreSQL
        assertThat(queryInventoryReservationStatusInPostgres(capResId)).isEqualTo("COMMITTED");
        assertThat(queryCapacityReservationStatusInPostgres(capResId)).isEqualTo("COMMITTED");

        // 7. Verificar pedido CONFIRMED en PostgreSQL
        Order persistedOrder = orderRepository.findById(scenario1OrderId).orElseThrow();
        assertThat(persistedOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(queryOrderStatusInPostgres(scenario1OrderId)).isEqualTo("CONFIRMED");

        // 8. Consultar inventario mediante servicio Merchant
        InventoryResponse finalMerchantView = getVariantInventoryService.execute(STORE_A_ID, scenario1VariantId);
        assertThat(finalMerchantView.physicalQuantity()).isEqualTo(7);
        assertThat(finalMerchantView.reservedQuantity()).isEqualTo(0); // COMMITTED ya no es hold activo
        assertThat(finalMerchantView.availableQuantity()).isEqualTo(7);

        // Verificar en listado de tienda del comerciante
        List<InventoryResponse> storeInventory = getStoreInventoryService.execute(STORE_A_ID);
        Optional<InventoryResponse> foundInStore = storeInventory.stream()
                .filter(i -> i.variantId().equals(scenario1VariantId))
                .findFirst();
        assertThat(foundInStore).isPresent();
        assertThat(foundInStore.get().physicalQuantity()).isEqualTo(7);
        assertThat(foundInStore.get().availableQuantity()).isEqualTo(7);
    }

    // =========================================================================
    // ESCENARIO 2: CANCELACIÓN Y RESTITUCIÓN
    // =========================================================================

    @Test
    @org.junit.jupiter.api.Order(2)
    @DisplayName("Escenario 2: Cancelación de pedido CONFIRMED y restitución de stock a 10 con idempotencia")
    void scenario2_shouldCancelOrderAndRestockWithoutDoubleRestitution() throws Exception {
        // 1. Partir del pedido confirmado del Escenario 1
        assertThat(scenario1OrderId).isNotNull();
        assertThat(queryOrderStatusInPostgres(scenario1OrderId)).isEqualTo("CONFIRMED");
        assertThat(queryPhysicalQuantityInPostgres(scenario1VariantId)).isEqualTo(7);

        // 2. Cancelar mediante el caso de uso real del cliente
        CustomerOrderDetailResponse cancelResponse = cancelCustomerOrderService.execute(
                CUSTOMER_USER_ID,
                scenario1OrderNumber,
                "Cliente cancela por cambio de planes"
        );
        assertThat(cancelResponse.status()).isEqualTo(OrderStatus.CANCELLED);

        // 3. Verificar stock físico restaurado a 10 en PostgreSQL
        assertThat(queryPhysicalQuantityInPostgres(scenario1VariantId)).isEqualTo(10);

        // 4. Verificar transición de reservas a RELEASED en PostgreSQL
        assertThat(queryInventoryReservationStatusInPostgres(scenario1CapResId)).isEqualTo("RELEASED");
        assertThat(queryCapacityReservationStatusInPostgres(scenario1CapResId)).isEqualTo("RELEASED");

        // Verificar pago con refundStatus PENDING
        Payment payment = paymentRepository.findById(scenario1PaymentId).orElseThrow();
        assertThat(payment.getRefundStatus()).isEqualTo(RefundStatus.PENDING);

        // 5. Repetir cancelación / compensación (Prueba de idempotencia)
        CustomerOrderDetailResponse secondCancel = cancelCustomerOrderService.execute(
                CUSTOMER_USER_ID,
                scenario1OrderNumber,
                "Intento de cancelación duplicada"
        );
        assertThat(secondCancel.status()).isEqualTo(OrderStatus.CANCELLED);

        // Invocar directamente el compensador transaccional sobre la orden ya cancelada
        Order cancelledOrder = orderRepository.findById(scenario1OrderId).orElseThrow();
        orderCancellationCompensator.compensate(cancelledOrder, OrderStatus.CONFIRMED, "Compensación duplicada manual");

        // 6. Confirmar que no exista doble restitución (el stock debe permanecer estrictamente en 10, no 13 ni 16)
        assertThat(queryPhysicalQuantityInPostgres(scenario1VariantId)).isEqualTo(10);

        InventoryResponse merchantAfterCancel = getVariantInventoryService.execute(STORE_A_ID, scenario1VariantId);
        assertThat(merchantAfterCancel.physicalQuantity()).isEqualTo(10);
        assertThat(merchantAfterCancel.availableQuantity()).isEqualTo(10);
        assertThat(merchantAfterCancel.reservedQuantity()).isEqualTo(0);
    }

    // =========================================================================
    // ESCENARIO 3: CANCELACIÓN EN PREPARACIÓN
    // =========================================================================

    @Test
    @org.junit.jupiter.api.Order(3)
    @DisplayName("Escenario 3: Pedido en IN_PREPARATION cancelado por comerciante NO restituye stock físico (RN-ORD-13)")
    void scenario3_shouldNotRestockWhenCancelledInPreparation() throws Exception {
        // 1. Crear producto, variante y pedido CONFIRMED independiente
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        Product product = createProductService.execute(new CreateProductCommand(
                STORE_A_ID,
                CATEGORY_A_ID,
                "Torta Fresa " + uniqueSuffix,
                "torta-fresa-" + uniqueSuffix,
                "Descripción fresa",
                null
        ));
        createdProductIds.add(product.getId());

        ProductVariant variant = createProductVariantService.execute(new CreateProductVariantCommand(
                STORE_A_ID,
                product.getId(),
                "Entera 12 Porciones",
                "SKU-FRESA-" + uniqueSuffix,
                new BigDecimal("80.00"),
                "PEN",
                true,
                Collections.emptyList()
        ));
        UUID variantId = variant.getId();
        createdVariantIds.add(variantId);

        // Configurar stock físico en 10
        updateInventoryStockService.execute(STORE_A_ID, variantId, 10);

        // Reservar y confirmar 4 unidades (Stock restante: 6)
        UUID capResId = UUID.randomUUID();
        createdCapResIds.add(capResId);
        Instant expiresAt = Instant.now().plus(20, ChronoUnit.MINUTES);
        seedCapacityReservationInPostgres(capResId, STORE_A_ID, CUSTOMER_USER_ID, "ACTIVE", expiresAt, null);

        CheckoutItemSnapshot itemSnapshot = createSnapshot(
                variantId,
                product.getId(),
                "Torta Fresa " + uniqueSuffix,
                4,
                new BigDecimal("80.00")
        );
        reserveInventoryService.execute(STORE_A_ID, capResId, expiresAt, List.of(itemSnapshot));

        // Proteger pago y confirmar compra
        updateCapacityReservationStatusInPostgres(capResId, "PAYMENT_PROTECTED", Instant.now().plus(10, ChronoUnit.MINUTES));
        UUID paymentId = UUID.randomUUID();
        createdPaymentIds.add(paymentId);
        seedPaymentInPostgres(paymentId, capResId, "APPROVED", new BigDecimal("320.00"), jsonMapper.writeValueAsString(List.of(itemSnapshot)));

        OrderConfirmation confirmation = confirmPurchaseService.execute(new ConfirmPurchaseCommand(paymentId));
        UUID orderId = confirmation.orderId();
        createdOrderIds.add(orderId);

        // Comprobar estado CONFIRMED y stock físico 6 en PostgreSQL
        assertThat(queryPhysicalQuantityInPostgres(variantId)).isEqualTo(6);
        assertThat(queryOrderStatusInPostgres(orderId)).isEqualTo("CONFIRMED");

        // 2. Avanzar pedido a IN_PREPARATION por el comerciante
        changeOrderStatusService.execute(orderId, OrderStatus.IN_PREPARATION, STORE_A_ID);
        assertThat(queryOrderStatusInPostgres(orderId)).isEqualTo("IN_PREPARATION");

        // 3. Ejecutar la cancelación y verificar reglas
        // 3a. El cliente intenta cancelar: debe ser RECHAZADO según RN-ORD-09
        assertThatThrownBy(() -> cancelCustomerOrderService.execute(CUSTOMER_USER_ID, confirmation.orderNumber(), "Intento cliente"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getCode()).isEqualTo("RN-ORD-09"));

        // 3b. El comerciante ejecuta la cancelación permitida
        changeOrderStatusService.execute(orderId, OrderStatus.CANCELLED, STORE_A_ID);
        assertThat(queryOrderStatusInPostgres(orderId)).isEqualTo("CANCELLED");

        // 4. Verificar que NO se restituya stock físico: permanece en 6 (NO se restaura a 10)
        assertThat(queryPhysicalQuantityInPostgres(variantId)).isEqualTo(6);

        // 5. Confirmar consistencia de reservas y pedido en PostgreSQL
        // Las reservas permanecen COMMITTED porque la preparación consumió físicamente los insumos
        assertThat(queryInventoryReservationStatusInPostgres(capResId)).isEqualTo("COMMITTED");
        assertThat(queryCapacityReservationStatusInPostgres(capResId)).isEqualTo("COMMITTED");

        // Vista Merchant confirma disponible = 6
        InventoryResponse merchantView = getVariantInventoryService.execute(STORE_A_ID, variantId);
        assertThat(merchantView.physicalQuantity()).isEqualTo(6);
        assertThat(merchantView.availableQuantity()).isEqualTo(6);
        assertThat(merchantView.reservedQuantity()).isEqualTo(0);
    }

    // =========================================================================
    // ESCENARIO 4: CONCURRENCIA REAL Y PREVENCIÓN DE SOBREVENTA
    // =========================================================================

    @Test
    @org.junit.jupiter.api.Order(4)
    @DisplayName("Escenario 4: Concurrencia real en PostgreSQL: 2 reservas simultáneas por 1 única unidad -> 1 éxito, 1 fallo, sin sobreventa")
    void scenario4_shouldPreventOversellingUnderRealConcurrency() throws Exception {
        // 1. Configurar una variante con 1 unidad disponible
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        Product product = createProductService.execute(new CreateProductCommand(
                STORE_A_ID,
                CATEGORY_A_ID,
                "Pie Limón " + uniqueSuffix,
                "pie-limon-" + uniqueSuffix,
                "Stock limitado",
                null
        ));
        createdProductIds.add(product.getId());

        ProductVariant variant = createProductVariantService.execute(new CreateProductVariantCommand(
                STORE_A_ID,
                product.getId(),
                "Porción Individual",
                "SKU-PIE-" + uniqueSuffix,
                new BigDecimal("15.00"),
                "PEN",
                true,
                Collections.emptyList()
        ));
        UUID variantId = variant.getId();
        createdVariantIds.add(variantId);

        updateInventoryStockService.execute(STORE_A_ID, variantId, 1);
        assertThat(queryPhysicalQuantityInPostgres(variantId)).isEqualTo(1);

        // 2 reservas de capacidad independientes
        UUID capRes1 = UUID.randomUUID();
        UUID capRes2 = UUID.randomUUID();
        createdCapResIds.add(capRes1);
        createdCapResIds.add(capRes2);
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);

        seedCapacityReservationInPostgres(capRes1, STORE_A_ID, CUSTOMER_USER_ID, "ACTIVE", expiresAt, null);
        seedCapacityReservationInPostgres(capRes2, STORE_A_ID, MERCHANT_B_USER_ID, "ACTIVE", expiresAt, null);

        CheckoutItemSnapshot itemSnapshot = createSnapshot(
                variantId,
                product.getId(),
                "Pie Limón " + uniqueSuffix,
                1,
                new BigDecimal("15.00")
        );

        // 2. Ejecutar dos intentos concurrentes sincronizados
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        Callable<Void> task1 = () -> {
            startLatch.await();
            try {
                reserveInventoryService.execute(STORE_A_ID, capRes1, expiresAt, List.of(itemSnapshot));
                successCount.incrementAndGet();
            } catch (ConflictException ex) {
                conflictCount.incrementAndGet();
            }
            return null;
        };

        Callable<Void> task2 = () -> {
            startLatch.await();
            try {
                reserveInventoryService.execute(STORE_A_ID, capRes2, expiresAt, List.of(itemSnapshot));
                successCount.incrementAndGet();
            } catch (ConflictException ex) {
                conflictCount.incrementAndGet();
            }
            return null;
        };

        Future<Void> f1 = executor.submit(task1);
        Future<Void> f2 = executor.submit(task2);

        startLatch.countDown(); // Disparo simultáneo

        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // 3. Verificar que solamente uno tenga éxito y el otro falle por stock insuficiente
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);

        // 4. Verificar que no exista sobreventa en PostgreSQL
        int activeReservedQuantity = inventoryJpaRepository.sumActiveReservedQuantity(STORE_A_ID, variantId, Instant.now());
        assertThat(activeReservedQuantity).isEqualTo(1);

        InventoryResponse currentInventory = getVariantInventoryService.execute(STORE_A_ID, variantId);
        assertThat(currentInventory.physicalQuantity()).isEqualTo(1);
        assertThat(currentInventory.reservedQuantity()).isEqualTo(1);
        assertThat(currentInventory.availableQuantity()).isEqualTo(0);

        // 5. Comprobar reservas en PostgreSQL: exactamente 1 activa
        int activeReservationCount = countActiveReservationsInPostgres(variantId);
        assertThat(activeReservationCount).isEqualTo(1);
    }

    // =========================================================================
    // ESCENARIO 5: EXPIRACIÓN DE RESERVAS Y LIBERACIÓN DINÁMICA
    // =========================================================================

    @Test
    @org.junit.jupiter.api.Order(5)
    @DisplayName("Escenario 5: Expiración de reservas: consulta dinámica excluye holds vencidos y permite nueva reserva inmediata")
    void scenario5_shouldHandleReservationExpirationAndReallocation() throws Exception {
        // 1. Crear producto con stock inicial 5
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        Product product = createProductService.execute(new CreateProductCommand(
                STORE_A_ID,
                CATEGORY_A_ID,
                "Cheesecake " + uniqueSuffix,
                "cheesecake-" + uniqueSuffix,
                "Prueba expiración",
                null
        ));
        createdProductIds.add(product.getId());

        ProductVariant variant = createProductVariantService.execute(new CreateProductVariantCommand(
                STORE_A_ID,
                product.getId(),
                "Porción 200g",
                "SKU-CHEE-" + uniqueSuffix,
                new BigDecimal("22.00"),
                "PEN",
                true,
                Collections.emptyList()
        ));
        UUID variantId = variant.getId();
        createdVariantIds.add(variantId);

        updateInventoryStockService.execute(STORE_A_ID, variantId, 5);

        // Crear una reserva inicial de 3 unidades
        UUID expiredCapResId = UUID.randomUUID();
        createdCapResIds.add(expiredCapResId);
        seedCapacityReservationInPostgres(expiredCapResId, STORE_A_ID, CUSTOMER_USER_ID, "ACTIVE", Instant.now().plusSeconds(600), null);

        CheckoutItemSnapshot item3 = createSnapshot(variantId, product.getId(), "Cheesecake", 3, new BigDecimal("22.00"));
        reserveInventoryService.execute(STORE_A_ID, expiredCapResId, Instant.now().plusSeconds(600), List.of(item3));

        // Verificar hold vigente: 5 físico - 3 reservado = 2 disponible
        InventoryResponse beforeExpiry = getVariantInventoryService.execute(STORE_A_ID, variantId);
        assertThat(beforeExpiry.availableQuantity()).isEqualTo(2);

        // 2. Simular vencimiento en PostgreSQL ajustando timestamps de expiración al pasado
        simulateExpirationInPostgres(expiredCapResId);

        // 3. Consultar disponibilidad Merchant
        InventoryResponse afterExpiry = getVariantInventoryService.execute(STORE_A_ID, variantId);

        // 4. Confirmar que la reserva vencida NO reduce el stock disponible
        assertThat(afterExpiry.physicalQuantity()).isEqualTo(5);
        assertThat(afterExpiry.reservedQuantity()).isEqualTo(0); // Excluida lógicamente por filtro temporal
        assertThat(afterExpiry.availableQuantity()).isEqualTo(5);

        // 5. Verificar que un nuevo cliente puede reservar las 5 unidades completas
        UUID newCapResId = UUID.randomUUID();
        createdCapResIds.add(newCapResId);
        Instant futureExpires = Instant.now().plus(15, ChronoUnit.MINUTES);
        seedCapacityReservationInPostgres(newCapResId, STORE_A_ID, MERCHANT_B_USER_ID, "ACTIVE", futureExpires, null);

        CheckoutItemSnapshot item5 = createSnapshot(variantId, product.getId(), "Cheesecake", 5, new BigDecimal("22.00"));
        List<InventoryReservation> newReservations = reserveInventoryService.execute(
                STORE_A_ID,
                newCapResId,
                futureExpires,
                List.of(item5)
        );
        assertThat(newReservations).hasSize(1);
        assertThat(newReservations.getFirst().getQuantity()).isEqualTo(5);

        // 6. Distinguir liberación lógica por consulta de transición persistida a EXPIRED
        // En PostgreSQL, la fila antigua aún tiene status = 'ACTIVE' antes de la transición explícita
        assertThat(queryInventoryReservationStatusInPostgres(expiredCapResId)).isEqualTo("ACTIVE");

        // Transicionar explícitamente la reserva antigua a EXPIRED
        List<InventoryReservation> oldReservations = inventoryReservationRepository.findByCapacityReservationId(expiredCapResId);
        assertThat(oldReservations).hasSize(1);
        InventoryReservation oldReservation = oldReservations.getFirst();
        oldReservation.expire();
        inventoryReservationRepository.save(oldReservation);

        // Confirmar estado persistido en PostgreSQL = EXPIRED
        assertThat(queryInventoryReservationStatusInPostgres(expiredCapResId)).isEqualTo("EXPIRED");
    }

    // =========================================================================
    // INVARIANTES EXPLÍCITOS (PASO 4)
    // =========================================================================

    @Test
    @org.junit.jupiter.api.Order(6)
    @DisplayName("Paso 4: Comprobación exhaustiva de Invariantes del Inventario")
    void invariants_shouldEnforceAllSystemInvariants() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        Product productA = createProductService.execute(new CreateProductCommand(
                STORE_A_ID, CATEGORY_A_ID, "Invariante Prod " + uniqueSuffix, "inv-prod-" + uniqueSuffix, "Desc", null
        ));
        createdProductIds.add(productA.getId());

        ProductVariant variantA = createProductVariantService.execute(new CreateProductVariantCommand(
                STORE_A_ID, productA.getId(), "Var A", "SKU-INV-A-" + uniqueSuffix, new BigDecimal("40.00"), "PEN", true, Collections.emptyList()
        ));
        UUID variantAId = variantA.getId();
        createdVariantIds.add(variantAId);

        updateInventoryStockService.execute(STORE_A_ID, variantAId, 10);

        // Invariante 1: No sobreventa (solicitar 11 cuando hay 10 disponible debe fallar con INSUFFICIENT_STOCK)
        UUID capResId = UUID.randomUUID();
        createdCapResIds.add(capResId);
        seedCapacityReservationInPostgres(capResId, STORE_A_ID, CUSTOMER_USER_ID, "ACTIVE", Instant.now().plusSeconds(600), null);

        CheckoutItemSnapshot excessiveItem = createSnapshot(variantAId, productA.getId(), "Var A", 11, new BigDecimal("40.00"));
        assertThatThrownBy(() -> reserveInventoryService.execute(STORE_A_ID, capResId, Instant.now().plusSeconds(600), List.of(excessiveItem)))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode()).isEqualTo("INSUFFICIENT_STOCK"));

        // Invariante 2: Aislamiento multitienda
        // La Tienda B no puede consultar ni operar sobre la variante de la Tienda A
        assertThatThrownBy(() -> getVariantInventoryService.execute(STORE_B_ID, variantAId))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> updateInventoryStockService.execute(STORE_B_ID, variantAId, 20))
                .isInstanceOf(ResourceNotFoundException.class);

        // Invariante 3: Conservación de PAYMENT_PROTECTED
        // Si expiresAt venció pero paymentProtectionExpiresAt sigue vigente, la reserva NO se libera
        UUID protectedCapResId = UUID.randomUUID();
        createdCapResIds.add(protectedCapResId);
        Instant pastExpires = Instant.now().minusSeconds(120);
        Instant activeProtection = Instant.now().plusSeconds(300);

        seedCapacityReservationInPostgres(protectedCapResId, STORE_A_ID, CUSTOMER_USER_ID, "PAYMENT_PROTECTED", pastExpires, activeProtection);

        CheckoutItemSnapshot protectedItem = createSnapshot(variantAId, productA.getId(), "Var A", 3, new BigDecimal("40.00"));
        reserveInventoryService.execute(STORE_A_ID, protectedCapResId, pastExpires, List.of(protectedItem));

        InventoryResponse protectedAvailability = getVariantInventoryService.execute(STORE_A_ID, variantAId);
        assertThat(protectedAvailability.physicalQuantity()).isEqualTo(10);
        assertThat(protectedAvailability.reservedQuantity()).isEqualTo(3);
        assertThat(protectedAvailability.availableQuantity()).isEqualTo(7);

        // Invariante 4: No doble descuento en confirmación (idempotencia)
        UUID paymentId = UUID.randomUUID();
        createdPaymentIds.add(paymentId);
        seedPaymentInPostgres(paymentId, protectedCapResId, "APPROVED", new BigDecimal("120.00"), jsonMapper.writeValueAsString(List.of(protectedItem)));

        OrderConfirmation conf1 = confirmPurchaseService.execute(new ConfirmPurchaseCommand(paymentId));
        createdOrderIds.add(conf1.orderId());
        assertThat(queryPhysicalQuantityInPostgres(variantAId)).isEqualTo(7);

        // Segundo intento de confirmación sobre el mismo pago
        OrderConfirmation conf2 = confirmPurchaseService.execute(new ConfirmPurchaseCommand(paymentId));
        assertThat(conf2.orderId()).isEqualTo(conf1.orderId());
        assertThat(queryPhysicalQuantityInPostgres(variantAId)).isEqualTo(7); // Permanece en 7, sin doble descuento

        // Invariante 5: Disponibilidad calculada correctamente
        InventoryResponse verifiedResponse = getVariantInventoryService.execute(STORE_A_ID, variantAId);
        assertThat(verifiedResponse.availableQuantity())
                .isEqualTo(verifiedResponse.physicalQuantity() - verifiedResponse.reservedQuantity());
    }

    // =========================================================================
    // PASO 5: VERIFICACIÓN HTTP REPRESENTATIVA
    // =========================================================================

    @Test
    @org.junit.jupiter.api.Order(7)
    @DisplayName("Paso 5: Verificación HTTP representativa sobre endpoints REST de inventario con autenticación JWT real")
    void step5_shouldVerifyMerchantInventoryOverHttp() throws Exception {
        String merchantAToken = jwtService.generateToken(MERCHANT_A_USER_ID, Set.of("MERCHANT"));
        String merchantBToken = jwtService.generateToken(MERCHANT_B_USER_ID, Set.of("MERCHANT"));

        // 1. GET /api/v1/inventory/variants/{variantId} para Tienda A
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/inventory/variants/" + scenario1VariantId))
                .header("Authorization", "Bearer " + merchantAToken)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(getResponse.statusCode()).isEqualTo(200);

        JsonNode getJson = jsonMapper.readTree(getResponse.body());
        assertThat(getJson.get("variantId").asText()).isEqualTo(scenario1VariantId.toString());
        assertThat(getJson.get("physicalQuantity").asInt()).isEqualTo(10);
        assertThat(getJson.get("availableQuantity").asInt()).isEqualTo(10);

        // 2. PUT /api/v1/inventory/variants/{variantId}/stock para ajustar stock físicamente
        String putBody = """
                {
                    "quantity": 18
                }
                """;

        HttpRequest putRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/inventory/variants/" + scenario1VariantId + "/stock"))
                .header("Authorization", "Bearer " + merchantAToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(putBody))
                .build();

        HttpResponse<String> putResponse = httpClient.send(putRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(putResponse.statusCode()).isEqualTo(200);

        JsonNode putJson = jsonMapper.readTree(putResponse.body());
        assertThat(putJson.get("physicalQuantity").asInt()).isEqualTo(18);
        assertThat(putJson.get("availableQuantity").asInt()).isEqualTo(18);

        // Verificar en PostgreSQL real que el ajuste persistió
        assertThat(queryPhysicalQuantityInPostgres(scenario1VariantId)).isEqualTo(18);

        // 3. Aislamiento multitienda HTTP: Merchant B intentando acceder a variante de Merchant A recibe 404
        HttpRequest forbiddenRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/inventory/variants/" + scenario1VariantId))
                .header("Authorization", "Bearer " + merchantBToken)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> forbiddenResponse = httpClient.send(forbiddenRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(forbiddenResponse.statusCode()).isEqualTo(404);
    }

    // =========================================================================
    // UTILIDADES SQL AUXILIARES PARA VERIFICACIÓN DIRECTA EN POSTGRESQL
    // =========================================================================

    private int queryPhysicalQuantityInPostgres(UUID variantId) throws SQLException {
        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password());
             PreparedStatement ps = conn.prepareStatement("SELECT quantity FROM inventories WHERE variant_id = ?")) {
            ps.setObject(1, variantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("quantity");
                }
            }
        }
        return -1;
    }

    private String queryInventoryReservationStatusInPostgres(UUID capResId) throws SQLException {
        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password());
             PreparedStatement ps = conn.prepareStatement("SELECT status FROM inventory_reservations WHERE capacity_reservation_id = ? LIMIT 1")) {
            ps.setObject(1, capResId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("status");
                }
            }
        }
        return null;
    }

    private String queryCapacityReservationStatusInPostgres(UUID capResId) throws SQLException {
        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password());
             PreparedStatement ps = conn.prepareStatement("SELECT status FROM capacity_reservations WHERE id = ?")) {
            ps.setObject(1, capResId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("status");
                }
            }
        }
        return null;
    }

    private String queryOrderStatusInPostgres(UUID orderId) throws SQLException {
        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password());
             PreparedStatement ps = conn.prepareStatement("SELECT status FROM orders WHERE id = ?")) {
            ps.setObject(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("status");
                }
            }
        }
        return null;
    }

    private int countActiveReservationsInPostgres(UUID variantId) throws SQLException {
        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password());
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM inventory_reservations WHERE variant_id = ? AND status = 'ACTIVE'")) {
            ps.setObject(1, variantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private void seedCapacityReservationInPostgres(UUID capResId, UUID storeId, UUID userId, String status, Instant expiresAt, Instant protectionExpiresAt) throws SQLException {
        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password());
             PreparedStatement ps = conn.prepareStatement("""
                     INSERT INTO capacity_reservations (id, store_id, user_id, service_date, start_time, end_time, expires_at, payment_protection_expires_at, status, created_at, updated_at)
                     VALUES (?, ?, ?, CURRENT_DATE + INTERVAL '5 days', '10:00:00', '11:00:00', ?, ?, ?, NOW(), NOW())
                     """)) {
            ps.setObject(1, capResId);
            ps.setObject(2, storeId);
            ps.setObject(3, userId);
            ps.setTimestamp(4, java.sql.Timestamp.from(expiresAt));
            ps.setTimestamp(5, protectionExpiresAt != null ? java.sql.Timestamp.from(protectionExpiresAt) : null);
            ps.setString(6, status);
            ps.executeUpdate();
        }
    }

    private void updateCapacityReservationStatusInPostgres(UUID capResId, String status, Instant protectionExpiresAt) throws SQLException {
        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password());
             PreparedStatement ps = conn.prepareStatement("""
                     UPDATE capacity_reservations
                     SET status = ?, payment_protection_expires_at = ?, updated_at = NOW()
                     WHERE id = ?
                     """)) {
            ps.setString(1, status);
            ps.setTimestamp(2, protectionExpiresAt != null ? java.sql.Timestamp.from(protectionExpiresAt) : null);
            ps.setObject(3, capResId);
            ps.executeUpdate();
        }
    }

    private void simulateExpirationInPostgres(UUID capResId) throws SQLException {
        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            try (PreparedStatement ps = conn.prepareStatement("""
                    UPDATE capacity_reservations
                    SET expires_at = NOW() - INTERVAL '10 minutes', updated_at = NOW()
                    WHERE id = ?
                    """)) {
                ps.setObject(1, capResId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("""
                    UPDATE inventory_reservations
                    SET expires_at = NOW() - INTERVAL '10 minutes', updated_at = NOW()
                    WHERE capacity_reservation_id = ?
                    """)) {
                ps.setObject(1, capResId);
                ps.executeUpdate();
            }
        }
    }

    private void seedPaymentInPostgres(UUID paymentId, UUID capResId, String status, BigDecimal amount, String snapshotJson) throws SQLException {
        try (Connection conn = DriverManager.getConnection(database.url(), database.username(), database.password())) {
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO payments (id, capacity_reservation_id, status, refund_status, amount, currency, fulfillment_type,
                                          customer_name, customer_phone, customer_email, delivery_address,
                                          products_subtotal_amount, discount_amount, delivery_fee_amount,
                                          included_tax_amount, items_snapshot, approved_at, created_at, updated_at)
                    VALUES (?, ?, ?, 'NOT_REQUIRED', ?, 'PEN', 'PICKUP', 'Cliente Test', '+51999999999', 'cliente@test.com', NULL,
                            ?, 0, 0, 0, ?::jsonb, NOW(), NOW(), NOW())
                    """)) {
                ps.setObject(1, paymentId);
                ps.setObject(2, capResId);
                ps.setString(3, status);
                ps.setBigDecimal(4, amount);
                ps.setBigDecimal(5, amount);
                ps.setString(6, snapshotJson);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO payment_attempts (id, payment_id, attempt_number, provider, payment_method, idempotency_key, status, provider_payment_id, created_at, updated_at)
                    VALUES (gen_random_uuid(), ?, 1, 'MERCADO_PAGO', 'CARD', ?, 'APPROVED', ?, NOW(), NOW())
                    """)) {
                ps.setObject(1, paymentId);
                ps.setString(2, "idem-" + UUID.randomUUID());
                ps.setString(3, "mp-prov-" + UUID.randomUUID());
                ps.executeUpdate();
            }
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class SafeTestConfig {

        @Bean
        DataSource dataSource() {
            TestPostgresConfig config = TestPostgresConfig.fromEnvironment();
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setUrl(config.url());
            dataSource.setUsername(config.username());
            dataSource.setPassword(config.password());
            return dataSource;
        }

        @Bean
        @Primary
        RealtimeNotificationPublisher realtimeNotificationPublisher() {
            return Mockito.mock(RealtimeNotificationPublisher.class);
        }
    }
}

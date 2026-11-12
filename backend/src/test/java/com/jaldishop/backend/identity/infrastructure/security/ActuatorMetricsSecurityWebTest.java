package com.jaldishop.backend.identity.infrastructure.security;

import com.jaldishop.backend.BackendApplication;
import com.jaldishop.backend.identity.domain.enums.RoleName;
import com.jaldishop.backend.identity.domain.enums.UserStatus;
import com.jaldishop.backend.identity.domain.model.Role;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = BackendApplication.class,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
                "management.endpoints.web.exposure.include=health,metrics",
                "management.endpoint.health.show-details=never",
                "JWT_SECRET=c2VjdXJlX2p3dF9zZWNyZXRfa2V5X2Zvcl90ZXN0aW5nX3B1cnBvc2VzXzEyMzQ1Njc4OTA="
        }
)
class ActuatorMetricsSecurityWebTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    private MockMvc mockMvc;

    private final UUID adminId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();
    private final UUID merchantId = UUID.randomUUID();

    private User adminUser;
    private User customerUser;
    private User merchantUser;

    private String adminToken;
    private String customerToken;
    private String merchantToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        adminUser = new User(
                adminId,
                "Admin",
                "System",
                "admin@jaldishop.test",
                "encodedPass",
                UserStatus.ACTIVE,
                null,
                Instant.now(),
                Instant.now(),
                Set.of(new Role((short) 1, RoleName.ADMIN))
        );

        customerUser = new User(
                customerId,
                "Customer",
                "User",
                "customer@jaldishop.test",
                "encodedPass",
                UserStatus.ACTIVE,
                null,
                Instant.now(),
                Instant.now(),
                Set.of(new Role((short) 2, RoleName.CUSTOMER))
        );

        merchantUser = new User(
                merchantId,
                "Merchant",
                "Owner",
                "merchant@jaldishop.test",
                "encodedPass",
                UserStatus.ACTIVE,
                null,
                Instant.now(),
                Instant.now(),
                Set.of(new Role((short) 3, RoleName.MERCHANT))
        );

        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customerUser));
        when(userRepository.findById(merchantId)).thenReturn(Optional.of(merchantUser));

        adminToken = jwtService.generateToken(adminId, Set.of("ADMIN"));
        customerToken = jwtService.generateToken(customerId, Set.of("CUSTOMER"));
        merchantToken = jwtService.generateToken(merchantId, Set.of("MERCHANT"));
    }

    @Test
    @DisplayName("Con exposure include=health,metrics: anónimo no puede acceder a /actuator/metrics (4xx)")
    void whenUnauthenticatedUserCallsMetrics_shouldDenyAccess() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Con exposure include=health,metrics: token JWT CUSTOMER real es rechazado con 403 Forbidden")
    void whenCustomerWithRealJwtCallsMetrics_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/actuator/metrics")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Con exposure include=health,metrics: token JWT MERCHANT real es rechazado con 403 Forbidden")
    void whenMerchantWithRealJwtCallsMetrics_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/actuator/metrics")
                        .header("Authorization", "Bearer " + merchantToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Con exposure include=health,metrics: token JWT ADMIN real obtiene 200 OK y lista de métricas")
    void whenAdminWithRealJwtCallsMetrics_shouldReturnOkAndMetricNames() throws Exception {
        mockMvc.perform(get("/actuator/metrics")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray());
    }

    @Test
    @DisplayName("Con exposure include=health,metrics: ADMIN puede consultar métricas específicas de JVM y CPU")
    void whenAdminCallsSpecificJvmMetrics_shouldReturnDetails() throws Exception {
        mockMvc.perform(get("/actuator/metrics/jvm.memory.used")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("jvm.memory.used"))
                .andExpect(jsonPath("$.measurements").isArray());

        mockMvc.perform(get("/actuator/metrics/process.cpu.usage")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("process.cpu.usage"));

        mockMvc.perform(get("/actuator/metrics/jvm.threads.live")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("jvm.threads.live"));
    }

    @Test
    @DisplayName("Con exposure include=health,metrics: endpoints sensibles siguen denegados (403) incluso con token ADMIN")
    void whenAdminCallsSensitiveEndpoints_shouldDenyAccess() throws Exception {
        mockMvc.perform(get("/actuator/env")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/actuator/beans")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/actuator/configprops")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Contratos R.1 preservados: liveness y readiness responden 200 UP sin detalles")
    void whenProbesCalled_shouldPreserveContracts() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());

        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist());

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Jaldi Shop is running"));
    }
}

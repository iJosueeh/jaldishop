package com.jaldishop.backend.identity.infrastructure.security;

import com.jaldishop.backend.BackendApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = BackendApplication.class,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
                "management.endpoints.web.exposure.include=health",
                "management.endpoint.health.show-details=never",
                "JWT_SECRET=c2VjdXJlX2p3dF9zZWNyZXRfa2V5X2Zvcl90ZXN0aW5nX3B1cnBvc2VzXzEyMzQ1Njc4OTA="
        }
)
class ActuatorSecurityWebTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @DisplayName("Actuator: /actuator/health público responde 200 con status UP sin componentes ni detalles internos")
    void whenHealthEndpointCalled_shouldReturnUpWithoutInternalDetails() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(jsonPath("$.details").doesNotExist())
                .andExpect(jsonPath("$.db").doesNotExist())
                .andExpect(jsonPath("$.diskSpace").doesNotExist());
    }

    @Test
    @DisplayName("Actuator: /actuator raíz deniega acceso sin revelar endpoints disponibles")
    void whenActuatorDiscoveryCalled_shouldDenyAccessWithoutExposingLinks() throws Exception {
        mockMvc.perform(get("/actuator"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$._links").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist());
    }

    @Test
    @DisplayName("Actuator: /actuator/info deniega acceso sin revelar información de build/git/app")
    void whenActuatorInfoCalled_shouldDenyAccessWithoutExposingInfo() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.app").doesNotExist())
                .andExpect(jsonPath("$.git").doesNotExist())
                .andExpect(jsonPath("$.build").doesNotExist());
    }

    @Test
    @DisplayName("Actuator: /actuator/env deniega acceso sin revelar variables ni perfiles")
    void whenActuatorEnvCalled_shouldDenyAccessWithoutExposingEnvironment() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.propertySources").doesNotExist())
                .andExpect(jsonPath("$.activeProfiles").doesNotExist());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Actuator: /actuator/** deniega acceso incluso a usuarios autenticados con rol ADMIN")
    void whenAuthenticatedUserCallsActuatorInfo_shouldDenyAccess() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.app").doesNotExist())
                .andExpect(jsonPath("$.git").doesNotExist());
    }

    @Test
    @DisplayName("Rutas públicas: /api/v1/health permanece funcional respondiendo 200")
    void whenPublicApiHealthCalled_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Jaldi Shop is running"));
    }
}

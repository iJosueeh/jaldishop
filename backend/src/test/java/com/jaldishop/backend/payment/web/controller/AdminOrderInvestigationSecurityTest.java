package com.jaldishop.backend.payment.web.controller;

import com.jaldishop.backend.payment.application.investigation.ManualOrderInvestigationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(AdminOrderInvestigationSecurityTest.Config.class)
class AdminOrderInvestigationSecurityTest {
    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean ManualOrderInvestigationService service() { return mock(ManualOrderInvestigationService.class); }
        @Bean AdminOrderInvestigationController controller(ManualOrderInvestigationService service) {
            return new AdminOrderInvestigationController(service);
        }
    }
    @Autowired AdminOrderInvestigationController controller;

    @Test @WithMockUser(roles = "CUSTOMER")
    void customerCannotInvestigate() {
        assertThrows(AccessDeniedException.class, () -> controller.investigate(UUID.randomUUID()));
    }
    @Test @WithMockUser(roles = "MERCHANT")
    void merchantCannotInvestigate() {
        assertThrows(AccessDeniedException.class, () -> controller.investigate(UUID.randomUUID()));
    }
    @Test @WithMockUser(roles = "ADMIN")
    void adminCanInvestigate() {
        assertDoesNotThrow(() -> controller.investigate(UUID.randomUUID()));
    }
}

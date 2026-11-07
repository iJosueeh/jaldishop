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
        @Bean com.jaldishop.backend.payment.application.reconciliation.PaymentReconciliationService reconciliation() {
            return mock(com.jaldishop.backend.payment.application.reconciliation.PaymentReconciliationService.class);
        }
        @Bean AdminOrderInvestigationController controller(ManualOrderInvestigationService service,
                com.jaldishop.backend.payment.application.reconciliation.PaymentReconciliationService reconciliation) {
            return new AdminOrderInvestigationController(service, reconciliation);
        }
    }
    @Autowired AdminOrderInvestigationController controller;
    @Autowired com.jaldishop.backend.payment.application.reconciliation.PaymentReconciliationService reconciliation;

    @org.junit.jupiter.api.BeforeEach void resetInvocations() { reset(reconciliation); }

    @Test void unauthenticatedCannotReconcile() {
        assertThrows(org.springframework.security.authentication.AuthenticationCredentialsNotFoundException.class,
                () -> controller.reconcile(UUID.randomUUID()));
        verifyNoInteractions(reconciliation);
    }

    @Test @WithMockUser(roles = "ADMIN")
    void endpointMapsExactlyOneAttemptAndNeverPassesClientFinancialState() throws Exception {
        UUID id = UUID.randomUUID();
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/v1/admin/payments/attempts/{id}/reconcile-order", id)
                .contentType("application/json").content("{\"status\":\"APPROVED\",\"amount\":0}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        verify(reconciliation).reconcileOrderAttempt(id);
        verifyNoMoreInteractions(reconciliation);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/v1/admin/payments/attempts/not-a-uuid/reconcile-order"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        verifyNoMoreInteractions(reconciliation);
    }

    @Test @WithMockUser(roles = "CUSTOMER")
    void customerCannotInvestigate() {
        assertThrows(AccessDeniedException.class, () -> controller.investigate(UUID.randomUUID()));
        assertThrows(AccessDeniedException.class, () -> controller.reconcile(UUID.randomUUID()));
    }
    @Test @WithMockUser(roles = "MERCHANT")
    void merchantCannotInvestigate() {
        assertThrows(AccessDeniedException.class, () -> controller.investigate(UUID.randomUUID()));
        assertThrows(AccessDeniedException.class, () -> controller.reconcile(UUID.randomUUID()));
    }
    @Test @WithMockUser(roles = "ADMIN")
    void adminCanInvestigate() {
        assertDoesNotThrow(() -> controller.investigate(UUID.randomUUID()));
        assertDoesNotThrow(() -> controller.reconcile(UUID.randomUUID()));
    }
}

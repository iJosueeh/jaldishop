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
import java.util.Set;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import com.jaldishop.backend.shared.security.ratelimit.AuthenticatedRateLimits;
import com.jaldishop.backend.shared.security.ratelimit.RateLimits;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(AdminOrderInvestigationSecurityTest.Config.class)
class AdminOrderInvestigationSecurityTest {
    private static final JwtPrincipal ADMIN = new JwtPrincipal(UUID.randomUUID(), Set.of("ADMIN"));
    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean ManualOrderInvestigationService service() { return mock(ManualOrderInvestigationService.class); }
        @Bean com.jaldishop.backend.payment.application.reconciliation.PaymentReconciliationService reconciliation() {
            return mock(com.jaldishop.backend.payment.application.reconciliation.PaymentReconciliationService.class);
        }
        @Bean AdminOrderInvestigationController controller(ManualOrderInvestigationService service,
                com.jaldishop.backend.payment.application.reconciliation.PaymentReconciliationService reconciliation) {
            return new AdminOrderInvestigationController(service, reconciliation,
                    new AuthenticatedRateLimits(new RateLimits(new MockEnvironment())));
        }
    }
    @Autowired AdminOrderInvestigationController controller;
    @Autowired com.jaldishop.backend.payment.application.reconciliation.PaymentReconciliationService reconciliation;

    @org.junit.jupiter.api.BeforeEach void resetInvocations() { reset(reconciliation); }

    @Test void unauthenticatedCannotReconcile() {
        assertThrows(org.springframework.security.authentication.AuthenticationCredentialsNotFoundException.class,
                () -> controller.reconcile(UUID.randomUUID(), ADMIN));
        verifyNoInteractions(reconciliation);
    }

    @Test @WithMockUser(roles = "ADMIN")
    void endpointMapsExactlyOneAttemptAndNeverPassesClientFinancialState() throws Exception {
        UUID id = UUID.randomUUID();
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new org.springframework.web.method.support.HandlerMethodArgumentResolver() {
                    public boolean supportsParameter(org.springframework.core.MethodParameter p) {
                        return p.hasParameterAnnotation(org.springframework.security.core.annotation.AuthenticationPrincipal.class);
                    }
                    public Object resolveArgument(org.springframework.core.MethodParameter p,
                            org.springframework.web.method.support.ModelAndViewContainer m,
                            org.springframework.web.context.request.NativeWebRequest r,
                            org.springframework.web.bind.support.WebDataBinderFactory b) { return ADMIN; }
                }).build();
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
        assertThrows(AccessDeniedException.class, () -> controller.investigate(UUID.randomUUID(), ADMIN));
        assertThrows(AccessDeniedException.class, () -> controller.reconcile(UUID.randomUUID(), ADMIN));
    }
    @Test @WithMockUser(roles = "MERCHANT")
    void merchantCannotInvestigate() {
        assertThrows(AccessDeniedException.class, () -> controller.investigate(UUID.randomUUID(), ADMIN));
        assertThrows(AccessDeniedException.class, () -> controller.reconcile(UUID.randomUUID(), ADMIN));
    }
    @Test @WithMockUser(roles = "ADMIN")
    void adminCanInvestigate() {
        assertDoesNotThrow(() -> controller.investigate(UUID.randomUUID(), ADMIN));
        assertDoesNotThrow(() -> controller.reconcile(UUID.randomUUID(), ADMIN));
    }
}

package com.jaldishop.backend.ordering.web.controller;

import com.jaldishop.backend.ordering.application.result.MerchantOrderStatusCountsResponse;
import com.jaldishop.backend.ordering.application.service.ChangeOrderStatusService;
import com.jaldishop.backend.ordering.application.service.GetMerchantOrderDetailService;
import com.jaldishop.backend.ordering.application.service.GetMerchantOrderStatusCountsService;
import com.jaldishop.backend.ordering.application.service.ListMerchantOrdersService;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import com.jaldishop.backend.store.application.service.StoreContextService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(MerchantOrderSecurityEnforcementIntegrationTest.TestSecurityConfig.class)
class MerchantOrderSecurityEnforcementIntegrationTest {

    @Configuration
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean StoreContextService storeContextService() { return mock(StoreContextService.class); }
        @Bean ListMerchantOrdersService listMerchantOrdersService() { return mock(ListMerchantOrdersService.class); }
        @Bean GetMerchantOrderDetailService getMerchantOrderDetailService() { return mock(GetMerchantOrderDetailService.class); }
        @Bean ChangeOrderStatusService changeOrderStatusService() { return mock(ChangeOrderStatusService.class); }
        @Bean GetMerchantOrderStatusCountsService getMerchantOrderStatusCountsService() { return mock(GetMerchantOrderStatusCountsService.class); }

        @Bean
        MerchantOrderController merchantOrderController(
                StoreContextService storeContextService,
                ListMerchantOrdersService listMerchantOrdersService,
                GetMerchantOrderDetailService getMerchantOrderDetailService,
                ChangeOrderStatusService changeOrderStatusService,
                GetMerchantOrderStatusCountsService getMerchantOrderStatusCountsService
        ) {
            return new MerchantOrderController(
                    storeContextService,
                    listMerchantOrdersService,
                    getMerchantOrderDetailService,
                    changeOrderStatusService,
                    getMerchantOrderStatusCountsService
            );
        }
    }

    @Autowired
    private MerchantOrderController merchantOrderController;

    @Autowired
    private GetMerchantOrderStatusCountsService countsService;

    @Autowired
    private StoreContextService storeContextService;

    private static final UUID STORE_ID = UUID.randomUUID();
    private static final UUID MERCHANT_USER_ID = UUID.randomUUID();
    private static final JwtPrincipal MERCHANT_PRINCIPAL = new JwtPrincipal(MERCHANT_USER_ID, Set.of("MERCHANT"));

    @Test
    @WithMockUser(roles = "MERCHANT")
    @DisplayName("Rol MERCHANT autorizado para consultar contadores")
    void shouldAllowMerchantRoleToGetOrderCounts() {
        when(countsService.execute(STORE_ID, null)).thenReturn(MerchantOrderStatusCountsResponse.empty());

        assertDoesNotThrow(() -> merchantOrderController.getOrderCounts(MERCHANT_PRINCIPAL, STORE_ID, null));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("Rol CUSTOMER debe ser rechazado con AccessDeniedException (403)")
    void shouldDenyCustomerRoleToGetOrderCounts() {
        assertThrows(AccessDeniedException.class, () ->
                merchantOrderController.getOrderCounts(MERCHANT_PRINCIPAL, STORE_ID, null));
    }

    @Test
    @WithMockUser(roles = "OPERATIONAL_ADMIN")
    @DisplayName("Rol ADMIN sin rol MERCHANT debe ser rechazado con AccessDeniedException (403)")
    void shouldDenyAdminRoleToGetOrderCounts() {
        assertThrows(AccessDeniedException.class, () ->
                merchantOrderController.getOrderCounts(MERCHANT_PRINCIPAL, STORE_ID, null));
    }

    @Test
    @DisplayName("Usuario no autenticado debe ser rechazado antes de ejecutar método")
    void shouldDenyUnauthenticatedAccess() {
        assertThrows(Exception.class, () ->
                merchantOrderController.getOrderCounts(null, STORE_ID, null));
    }
}

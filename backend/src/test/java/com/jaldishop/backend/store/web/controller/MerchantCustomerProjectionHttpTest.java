package com.jaldishop.backend.store.web.controller;

import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import com.jaldishop.backend.store.application.service.*;
import com.jaldishop.backend.store.domain.enums.StoreStatus;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
import com.jaldishop.backend.store.infrastructure.persistence.adapter.StoreCustomerRepositoryAdapter;
import com.jaldishop.backend.store.infrastructure.persistence.projection.StoreCustomerProjection;
import com.jaldishop.backend.store.infrastructure.persistence.repository.StoreCustomerJpaRepository;
import com.jaldishop.backend.store.web.mapper.StoreCustomerResponseMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real Spring projection -> adapter -> service -> secured controller -> JSON; only database IO is mocked. */
@SpringJUnitConfig(MerchantCustomerProjectionHttpTest.Config.class)
class MerchantCustomerProjectionHttpTest {
    @Configuration(proxyBeanMethods = false)
    @EnableMethodSecurity
    @Import({StoreCustomerRepositoryAdapter.class, GetStoreCustomersService.class, GetMyStoreService.class,
            StoreContextService.class, StoreCustomerResponseMapper.class, MerchantCustomerController.class})
    static class Config {
        @Bean StoreCustomerJpaRepository customers() { return mock(StoreCustomerJpaRepository.class); }
        @Bean StoreRepository stores() { return mock(StoreRepository.class); }
    }
    @Autowired MerchantCustomerController controller;
    @Autowired StoreCustomerJpaRepository customers;
    @Autowired StoreRepository stores;
    private MockMvc mvc;
    private final UUID merchant = UUID.randomUUID(), store = UUID.randomUUID(), customer = UUID.randomUUID();
    private final Instant since = Instant.parse("2026-09-01T10:00:00.123456Z");

    @BeforeEach void setup() {
        reset(customers, stores);
        authenticate("MERCHANT");
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    @AfterEach void clearSecurity() { SecurityContextHolder.clearContext(); }

    @Test void returns200WithPreciseDatesNullsAndExistingContract() throws Exception {
        when(stores.findByMerchantUserId(merchant)).thenReturn(Optional.of(Store.reconstitute(
                store, merchant, "Store", "store", null, null, null, null, null, null,
                true, false, null, null, null, StoreStatus.ACTIVE, since, since)));
        var values = new HashMap<String, Object>(Map.of("userId", customer, "firstName", "Carlos",
                "lastName", "Garcia", "email", "carlos@example.test", "customerSince", since,
                "ordersCount", 2L, "totalSpent", new BigDecimal("30.75"),
                "lastOrderAt", Instant.parse("2026-09-15T15:30:00.654321Z")));
        var factory = new SpelAwareProxyProjectionFactory();
        var withOrders = factory.createProjection(StoreCustomerProjection.class, values);
        values = new HashMap<>(values);
        values.put("ordersCount", 0L); values.put("totalSpent", BigDecimal.ZERO); values.put("lastOrderAt", null);
        var withoutOrders = factory.createProjection(StoreCustomerProjection.class, values);
        when(customers.findCustomersByStoreId(store, null)).thenReturn(List.of(withOrders, withoutOrders));

        mvc.perform(get("/api/v1/merchant/customers").param("storeId", UUID.randomUUID().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].length()").value(9))
                .andExpect(jsonPath("$[0].userId").value(customer.toString()))
                .andExpect(jsonPath("$[0].customerSince").value(since.toString()))
                .andExpect(jsonPath("$[0].lastOrderAt").value("2026-09-15T15:30:00.654321Z"))
                .andExpect(jsonPath("$[0].ordersCount").value(2))
                .andExpect(jsonPath("$[0].totalSpentAmount").value(30.75))
                .andExpect(jsonPath("$[1].ordersCount").value(0))
                .andExpect(jsonPath("$[1].totalSpentAmount").value(0))
                .andExpect(jsonPath("$[1].lastOrderAt").value(org.hamcrest.Matchers.nullValue()));
        verify(customers).findCustomersByStoreId(store, null);
    }

    @Test void rejectsCustomerRoleBeforeDatabaseAccess() throws Exception {
        authenticate("CUSTOMER");
        mvc.perform(get("/api/v1/merchant/customers")).andExpect(status().isForbidden());
        verifyNoInteractions(stores, customers);
    }

    @Test void rejectsUnauthenticatedInvocationBeforeDatabaseAccess() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> controller.listCustomers(null, null))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        verifyNoInteractions(stores, customers);
    }

    @Test void preservesStoreNotFoundError() throws Exception {
        when(stores.findByMerchantUserId(merchant)).thenReturn(Optional.empty());
        mvc.perform(get("/api/v1/merchant/customers"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        verifyNoInteractions(customers);
    }

    private void authenticate(String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new JwtPrincipal(merchant, Set.of(role)), null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}

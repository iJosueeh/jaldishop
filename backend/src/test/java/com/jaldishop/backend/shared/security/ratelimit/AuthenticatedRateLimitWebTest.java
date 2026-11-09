package com.jaldishop.backend.shared.security.ratelimit;

import com.jaldishop.backend.capacity.application.availability.service.GetCapacityAvailabilityService;
import com.jaldishop.backend.capacity.application.reservation.service.*;
import com.jaldishop.backend.capacity.domain.model.CapacityReservation;
import com.jaldishop.backend.capacity.web.controller.CapacityReservationController;
import com.jaldishop.backend.checkout.application.service.InitiateCheckoutService;
import com.jaldishop.backend.checkout.application.result.CheckoutItemSnapshotAssembler;
import com.jaldishop.backend.capacity.application.availability.service.GetEffectiveCapacityService;
import com.jaldishop.backend.capacity.application.availability.result.EffectiveCapacityResolution;
import com.jaldishop.backend.capacity.application.availability.result.EffectiveCapacitySource;
import com.jaldishop.backend.capacity.domain.repository.*;
import com.jaldishop.backend.capacity.domain.model.CapacityConfiguration;
import com.jaldishop.backend.cart.domain.model.Cart;
import com.jaldishop.backend.cart.domain.repository.CartRepository;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.enums.StoreStatus;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
import com.jaldishop.backend.inventory.application.service.ReserveInventoryService;
import com.jaldishop.backend.checkout.web.controller.CheckoutController;
import com.jaldishop.backend.checkout.web.mapper.CheckoutResponseMapper;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.identity.infrastructure.security.*;
import com.jaldishop.backend.ordering.application.service.*;
import com.jaldishop.backend.ordering.web.controller.CustomerOrderController;
import com.jaldishop.backend.payment.application.investigation.ManualOrderInvestigationService;
import com.jaldishop.backend.payment.application.reconciliation.PaymentReconciliationService;
import com.jaldishop.backend.payment.web.controller.AdminOrderInvestigationController;
import com.jaldishop.backend.shared.exception.*;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(AuthenticatedRateLimitWebTest.Config.class)
@org.springframework.test.context.TestPropertySource(properties = "RENDER=true")
@WebAppConfiguration
class AuthenticatedRateLimitWebTest {
    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, JwtAuthenticationFilter.class, WebCorsConfiguration.class,
            RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, GlobalExceptionHandler.class,
            LoginRateLimitAdvice.class, RateLimitWebConfiguration.class, AuthenticatedRateLimits.class,
            CheckoutController.class, CapacityReservationController.class, CustomerOrderController.class,
            AdminOrderInvestigationController.class, CheckoutResponseMapper.class})
    static class Config {
        @Bean ObjectMapper mapper() { return new ObjectMapper(); }
        @Bean RateLimits limits() {
            // IP policies disabled as on Render: authenticated admission must still run.
            return new RateLimits(new MockEnvironment()
                    .withProperty("app.security.rate-limit.reservation-create-user.limit", "2")
                    .withProperty("app.security.rate-limit.payment-admin-remote-user.limit", "2"));
        }
        @Bean JwtService jwt() { return mock(JwtService.class); }
        @Bean UserRepository users() { return mock(UserRepository.class); }
        @Bean InitiateCheckoutService checkout() { return mock(InitiateCheckoutService.class); }
        @Bean GetCapacityAvailabilityService availability() { return mock(GetCapacityAvailabilityService.class); }
        @Bean CreateCapacityReservationService create() { return mock(CreateCapacityReservationService.class); }
        @Bean GetCapacityReservationService get() { return mock(GetCapacityReservationService.class); }
        @Bean ReleaseCapacityReservationService release() { return mock(ReleaseCapacityReservationService.class); }
        @Bean GetCustomerOrdersService orders() { return mock(GetCustomerOrdersService.class); }
        @Bean GetCustomerOrderDetailService detail() { return mock(GetCustomerOrderDetailService.class); }
        @Bean CancelCustomerOrderService cancel() { return mock(CancelCustomerOrderService.class); }
        @Bean ManualOrderInvestigationService investigate() { return mock(ManualOrderInvestigationService.class); }
        @Bean PaymentReconciliationService reconcile() { return mock(PaymentReconciliationService.class); }
    }

    @Autowired WebApplicationContext context;
    @Autowired JwtService jwt;
    @Autowired UserRepository users;
    @Autowired InitiateCheckoutService checkout;
    @Autowired CreateCapacityReservationService create;
    @Autowired ReleaseCapacityReservationService release;
    @Autowired CancelCustomerOrderService cancel;
    @Autowired ManualOrderInvestigationService investigate;
    @Autowired PaymentReconciliationService reconcile;
    @Autowired AuthenticatedRateLimits authenticatedLimits;
    private MockMvc mvc;
    private UUID userId;
    private String token;
    private final UUID storeId = UUID.randomUUID();

    @BeforeEach void setup() {
        reset(jwt, users, checkout, create, release, cancel, investigate, reconcile);
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity()).build();
        userId = UUID.randomUUID();
        token = authenticate(userId, "CUSTOMER");
        when(create.execute(any())).thenAnswer(inv -> CapacityReservation.create(
                storeId, userId, LocalDate.now().plusDays(1), LocalTime.NOON, LocalTime.of(13, 0)));
    }

    private String authenticate(UUID id, String role) {
        String bearer = UUID.randomUUID().toString();
        when(jwt.getPrincipal(bearer)).thenReturn(new JwtPrincipal(id, Set.of(role)));
        User user = mock(User.class);
        when(user.canAuthenticate()).thenReturn(true);
        var currentRole = mock(com.jaldishop.backend.identity.domain.model.Role.class);
        when(currentRole.getName()).thenReturn(com.jaldishop.backend.identity.domain.enums.RoleName.valueOf(role));
        when(user.getRoles()).thenReturn(Set.of(currentRole));
        when(users.findById(id)).thenReturn(Optional.of(user));
        return bearer;
    }

    private String body() {
        return """
                {"storeId":"%s","fulfillmentType":"PICKUP","serviceDate":"%s",
                 "startTime":"12:00","endTime":"13:00","userId":"%s"}
                """.formatted(storeId, LocalDate.now().plusDays(1), UUID.randomUUID());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder creation(String path, String bearer) {
        return post("/api/v1" + path).header("Authorization", "Bearer " + bearer)
                .contentType("application/json").content(body());
    }

    @Test void sharedBudgetUsesValidatedIdentityAcrossRoutesTokensAndSpoofedIds() throws Exception {
        mvc.perform(creation("/checkout", token)).andExpect(status().isCreated());
        // A refreshed token for the same user must share the first token's counter.
        String renewed = authenticate(userId, "CUSTOMER");
        mvc.perform(creation("/capacity/reservations", renewed)).andExpect(status().isCreated());
        mvc.perform(creation("/checkout", token).header("X-User-ID", UUID.randomUUID()))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));
        verify(checkout, times(1)).execute(argThat(c -> userId.equals(c.userId())));
        verify(create, times(1)).execute(argThat(c -> userId.equals(c.userId())));
        String other = authenticate(UUID.randomUUID(), "CUSTOMER");
        mvc.perform(creation("/checkout", other)).andExpect(status().isCreated());
    }

    @Test void invalidExpiredAndMissingJwtNeverConsumeAuthenticatedQuota() throws Exception {
        when(jwt.getPrincipal("invalid")).thenThrow(new MalformedJwtException("invalid"));
        when(jwt.getPrincipal("expired")).thenThrow(new ExpiredJwtException(null, null, "expired"));
        mvc.perform(creation("/checkout", "invalid")).andExpect(status().isUnauthorized());
        mvc.perform(creation("/capacity/reservations", "expired")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/checkout").contentType("application/json").content(body()))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(checkout, create);
        verify(users, never()).findById(argThat(id -> !userId.equals(id)));
        mvc.perform(creation("/checkout", token)).andExpect(status().isCreated());
        mvc.perform(creation("/capacity/reservations", token)).andExpect(status().isCreated());
    }

    @Test void realCheckoutAndInternalReservationConsumeOnlyOneAdmission() throws Exception {
        var effective = mock(GetEffectiveCapacityService.class);
        var configurations = mock(CapacityConfigurationRepository.class);
        var exceptions = mock(CapacityExceptionRepository.class);
        var reservations = mock(CapacityReservationRepository.class);
        UUID anchor = UUID.randomUUID();
        when(effective.resolve(any())).thenReturn(new EffectiveCapacityResolution(
                100, EffectiveCapacitySource.BASE, anchor, null));
        when(configurations.findByIdForUpdate(anchor)).thenReturn(Optional.of(mock(CapacityConfiguration.class)));
        when(reservations.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var realCreate = new CreateCapacityReservationService(effective, configurations, exceptions, reservations);
        when(create.execute(any())).thenAnswer(inv -> realCreate.execute(inv.getArgument(0)));
        var stores = mock(StoreRepository.class);
        var carts = mock(CartRepository.class);
        var snapshots = mock(CheckoutItemSnapshotAssembler.class);
        var inventory = mock(ReserveInventoryService.class);
        Store store = mock(Store.class);
        when(store.getId()).thenReturn(storeId);
        when(store.getStatus()).thenReturn(StoreStatus.ACTIVE);
        when(store.isPickupEnabled()).thenReturn(true);
        when(stores.findById(storeId)).thenReturn(Optional.of(store));
        Cart cart = Cart.create(userId, storeId);
        cart.addItem(UUID.randomUUID(), 1, java.math.BigDecimal.TEN, "PEN");
        when(carts.findByUserIdAndStoreId(userId, storeId)).thenReturn(Optional.of(cart));
        when(snapshots.assemble(cart, storeId)).thenReturn(List.of());
        var realCheckout = new InitiateCheckoutService(carts, stores, snapshots, realCreate, inventory);
        when(checkout.execute(any())).thenAnswer(inv -> realCheckout.execute(inv.getArgument(0)));

        mvc.perform(creation("/checkout", token)).andExpect(status().isCreated());
        mvc.perform(creation("/capacity/reservations", token)).andExpect(status().isCreated());
        mvc.perform(creation("/checkout", token)).andExpect(status().isTooManyRequests());
        verify(reservations, times(2)).save(any());
        verify(inventory, times(1)).execute(any(), any(), any(), any());
    }

    @Test void wrongRoleRetains403EvenAfterQuotaIsExhausted() throws Exception {
        String merchant = authenticate(userId, "MERCHANT");
        mvc.perform(creation("/capacity/reservations", merchant)).andExpect(status().isForbidden());
        authenticatedLimits.reservationCreation(userId);
        authenticatedLimits.reservationCreation(userId);
        mvc.perform(creation("/capacity/reservations", merchant)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/payments/attempts/{id}/investigate-order", UUID.randomUUID())
                .header("Authorization", "Bearer " + merchant)).andExpect(status().isForbidden());
        verifyNoInteractions(create, investigate);
    }

    @Test void concurrentHttpRequestsAdmitOnlyTheSharedBudget() {
        var admitted = new AtomicInteger();
        var rejected = new AtomicInteger();
        java.util.stream.IntStream.range(0, 12).parallel().forEach(i -> {
            try {
                int status = mvc.perform(creation(i % 2 == 0 ? "/checkout" : "/capacity/reservations", token))
                        .andReturn().getResponse().getStatus();
                if (status == 201) admitted.incrementAndGet();
                else { assertThat(status).isEqualTo(429); rejected.incrementAndGet(); }
            } catch (Exception ex) { throw new AssertionError(ex); }
        });
        assertThat(admitted.get()).isEqualTo(2);
        assertThat(rejected.get()).isEqualTo(10);
    }

    @Test void cancellationAndReleaseRemainAvailableAfterCreationIsExhausted() throws Exception {
        authenticatedLimits.reservationCreation(userId);
        authenticatedLimits.reservationCreation(userId);
        mvc.perform(creation("/checkout", token)).andExpect(status().isTooManyRequests());
        UUID reservationId = UUID.randomUUID();
        when(release.execute(reservationId, userId)).thenReturn(CapacityReservation.create(
                storeId, userId, LocalDate.now().plusDays(1), LocalTime.NOON, LocalTime.of(13, 0)));
        mvc.perform(delete("/api/v1/capacity/reservations/{id}", reservationId)
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(patch("/api/v1/customer/orders/ORDER-1/cancel")
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        verify(release).execute(reservationId, userId);
        verify(cancel).execute(userId, "ORDER-1", null);
        verifyNoInteractions(checkout);
    }

    @Test void administratorBudgetIsSharedAcrossRemoteOperationsAndIndependentOfCreationAndOtherAdmins() throws Exception {
        String admin = authenticate(userId, "ADMIN");
        authenticatedLimits.reservationCreation(userId);
        authenticatedLimits.reservationCreation(userId);
        String base = "/api/v1/admin/payments/attempts/" + UUID.randomUUID();
        mvc.perform(post(base + "/investigate-order").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        mvc.perform(post(base + "/reconcile-order").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        mvc.perform(post(base + "/investigate-order").header("Authorization", "Bearer " + admin))
                .andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
        String other = authenticate(UUID.randomUUID(), "ADMIN");
        mvc.perform(post(base + "/investigate-order").header("Authorization", "Bearer " + other))
                .andExpect(status().isOk());
        verify(investigate, times(2)).investigate(any());
        verify(reconcile, times(1)).reconcileOrderAttempt(any());
    }

    @Test void browserCanReadRetryAfterOnAnActualCors429() throws Exception {
        authenticatedLimits.reservationCreation(userId);
        authenticatedLimits.reservationCreation(userId);
        mvc.perform(creation("/checkout", token).header("Origin", "https://jaldishop.pe"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Access-Control-Expose-Headers", "Retry-After"))
                .andExpect(header().exists("Retry-After"));
        mvc.perform(options("/api/v1/checkout").header("Origin", "https://jaldishop.pe")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk());
    }
}

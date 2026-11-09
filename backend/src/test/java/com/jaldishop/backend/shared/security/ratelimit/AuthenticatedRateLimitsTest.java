package com.jaldishop.backend.shared.security.ratelimit;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.*;

class AuthenticatedRateLimitsTest {
    @Test
    void defaultPaymentQuotaIsSixAndDoesNotConsumeReservationOrAnotherUsersQuota() {
        var policies = new AuthenticatedRateLimits(new RateLimits(new MockEnvironment()));
        UUID user = UUID.randomUUID();
        for (int i = 0; i < 6; i++) policies.paymentCreation(user);
        assertThatThrownBy(() -> policies.paymentCreation(user))
                .isInstanceOfSatisfying(RateLimitExceeded.class,
                        failure -> assertThat(failure.retryAfter()).isBetween(1L, 60L));
        assertThatCode(() -> policies.reservationCreation(user)).doesNotThrowAnyException();
        assertThatCode(() -> policies.paymentCreation(UUID.randomUUID())).doesNotThrowAnyException();
    }

    @Test
    void sharedEngineRetainsQuotaAcrossFixturesWhileFreshEngineIsIndependent() {
        var environment = new MockEnvironment();
        var shared = new RateLimits(environment);
        UUID user = UUID.randomUUID();
        var firstFixture = new AuthenticatedRateLimits(shared);
        for (int i = 0; i < 6; i++) firstFixture.paymentCreation(user);
        var nextFixture = new AuthenticatedRateLimits(shared);
        assertThatThrownBy(() -> nextFixture.paymentCreation(user)).isInstanceOf(RateLimitExceeded.class);

        var isolatedFixture = new AuthenticatedRateLimits(new RateLimits(environment));
        for (int i = 0; i < 6; i++) isolatedFixture.paymentCreation(user);
        assertThatThrownBy(() -> isolatedFixture.paymentCreation(user)).isInstanceOf(RateLimitExceeded.class);
    }
}

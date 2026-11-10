package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.shared.security.ratelimit.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.core.task.*;
import java.util.ArrayList;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequestPasswordRecoveryServiceTest {
    @Test void normalizesAndQueuesWithoutAccountLookupAndSuppressesAccountLimitForAllAddresses() {
        var jobs = new ArrayList<Runnable>();
        var recovery = mock(PasswordRecoveryService.class);
        var request = new RequestPasswordRecoveryService(new RateLimits(new MockEnvironment()),
                new PasswordResetTokens(), jobs::add, recovery);
        for (int i = 0; i < 5; i++) request.request(" KNOWN@example.com ");
        for (int i = 0; i < 5; i++) request.request(" unknown@example.com ");
        assertThat(jobs).hasSize(6); verifyNoInteractions(recovery);
        jobs.forEach(Runnable::run);
        verify(recovery, times(3)).issue("known@example.com");
        verify(recovery, times(3)).issue("unknown@example.com");
    }
    @Test void queueCapacityReturnsRateLimitAndProviderFailureDoesNotReachHttpCaller() {
        var recovery = mock(PasswordRecoveryService.class);
        TaskExecutor full = task -> { throw new TaskRejectedException("full"); };
        var request = new RequestPasswordRecoveryService(new RateLimits(new MockEnvironment()),
                new PasswordResetTokens(), full, recovery);
        assertThatThrownBy(() -> request.request("user@example.com")).isInstanceOf(RateLimitExceeded.class);
        doThrow(new IllegalStateException("provider failure")).when(recovery).issue(anyString());
        var synchronous = new RequestPasswordRecoveryService(new RateLimits(new MockEnvironment()),
                new PasswordResetTokens(), Runnable::run, recovery);
        assertThatCode(() -> synchronous.request("user@example.com")).doesNotThrowAnyException();
    }
}

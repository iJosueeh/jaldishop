package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.application.command.ForgotPasswordCommand;
import com.jaldishop.backend.identity.application.port.PasswordRecoveryRepository;
import com.jaldishop.backend.identity.application.port.PasswordResetMail;
import com.jaldishop.backend.identity.domain.enums.RoleName;
import com.jaldishop.backend.identity.domain.model.Role;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.infrastructure.security.PasswordResetTokens;
import com.jaldishop.backend.shared.security.ratelimit.RateLimitExceeded;
import com.jaldishop.backend.shared.security.ratelimit.RateLimits;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForgotPasswordServiceTest {
    @Mock PasswordRecoveryRepository repository;
    @Mock PasswordResetMail mail;
    @Mock PlatformTransactionManager manager;
    private final PasswordResetTokens tokens = new PasswordResetTokens();
    private final Instant now = Instant.parse("2026-11-10T12:00:00Z");

    private ForgotPasswordService service(TaskExecutor executor) {
        return new ForgotPasswordService(new RateLimits(new MockEnvironment()), tokens, executor,
                repository, mail, new TransactionTemplate(manager), Clock.fixed(now, ZoneOffset.UTC));
    }
    private User user(String email) {
        return User.create(email, "encoded-password", "Test", "User", null,
                Set.of(new Role((short) 1, RoleName.CUSTOMER)));
    }
    private void transactions() {
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }
    @Test void issuesRandomHashedTokenForExactlyFifteenMinutesAndSendsAfterCommit() {
        transactions();
        User user = user("user@example.com");
        when(repository.lockByEmail(user.getEmail())).thenReturn(Optional.of(user));
        service(Runnable::run).execute(new ForgotPasswordCommand(" USER@EXAMPLE.COM "));
        var raw = ArgumentCaptor.forClass(String.class);
        verify(mail).send(eq(user.getEmail()), raw.capture(), eq(now.plusSeconds(900)));
        assertThat(raw.getValue()).matches("[A-Za-z0-9_-]{43}");
        verify(repository).replaceToken(user.getId(), tokens.hash(raw.getValue()), now.plusSeconds(900));
        var order = inOrder(manager, mail);
        order.verify(manager).commit(any());
        order.verify(mail).send(any(), any(), any());
        assertThat(tokens.generate()).isNotEqualTo(tokens.generate());
    }
    @Test void unknownAndInactiveUsersNeverReceiveMail() {
        transactions();
        User user = user("user@example.com");
        user.suspend(null);
        when(repository.lockByEmail("missing@example.com")).thenReturn(Optional.empty());
        when(repository.lockByEmail(user.getEmail())).thenReturn(Optional.of(user));
        var service = service(Runnable::run);
        service.execute(new ForgotPasswordCommand("missing@example.com"));
        service.execute(new ForgotPasswordCommand("user@example.com"));
        verifyNoInteractions(mail);
        verify(repository, never()).replaceToken(any(), any(), any());
    }
    @Test void normalizesAndQueuesWithoutAccountLookupAndSuppressesAccountLimitForAllAddresses() {
        transactions();
        User known = user("known@example.com");
        when(repository.lockByEmail(known.getEmail())).thenReturn(Optional.of(known));
        when(repository.lockByEmail("unknown@example.com")).thenReturn(Optional.empty());
        var jobs = new ArrayList<Runnable>();
        var service = service(jobs::add);
        for (int i = 0; i < 5; i++) service.execute(new ForgotPasswordCommand(" KNOWN@example.com "));
        for (int i = 0; i < 5; i++) service.execute(new ForgotPasswordCommand(" unknown@example.com "));
        assertThat(jobs).hasSize(6);
        verifyNoInteractions(repository, mail);
        jobs.forEach(Runnable::run);
        verify(repository, times(3)).lockByEmail("known@example.com");
        verify(repository, times(3)).lockByEmail("unknown@example.com");
        verify(mail, times(3)).send(eq(known.getEmail()), any(), any());
        verify(mail, never()).send(eq("unknown@example.com"), any(), any());
    }
    @Test void queueCapacityReturnsRateLimitAndProviderFailureDoesNotReachHttpCaller() {
        TaskExecutor full = task -> { throw new TaskRejectedException("full"); };
        assertThatThrownBy(() -> service(full).execute(new ForgotPasswordCommand("user@example.com")))
                .isInstanceOf(RateLimitExceeded.class);
        verifyNoInteractions(repository, mail);
        transactions();
        User user = user("user@example.com");
        when(repository.lockByEmail(user.getEmail())).thenReturn(Optional.of(user));
        doThrow(new IllegalStateException("provider failure")).when(mail).send(any(), any(), any());
        assertThatCode(() -> service(Runnable::run).execute(new ForgotPasswordCommand(user.getEmail())))
                .doesNotThrowAnyException();
        verify(repository).replaceToken(eq(user.getId()), any(), any());
        verify(manager).commit(any());
    }
}

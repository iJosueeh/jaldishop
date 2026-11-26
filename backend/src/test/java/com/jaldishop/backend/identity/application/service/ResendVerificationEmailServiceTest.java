package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.application.command.ResendVerificationCommand;
import com.jaldishop.backend.identity.application.port.EmailVerificationMail;
import com.jaldishop.backend.identity.application.port.EmailVerificationRepository;
import com.jaldishop.backend.identity.domain.enums.RoleName;
import com.jaldishop.backend.identity.domain.model.Role;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.infrastructure.security.OpaqueTokenCodec;
import com.jaldishop.backend.shared.security.ratelimit.RateLimits;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResendVerificationEmailServiceTest {

    @Mock private EmailVerificationRepository repository;
    @Mock private EmailVerificationMail mail;

    private final OpaqueTokenCodec tokens = new OpaqueTokenCodec();
    private final Instant now = Instant.parse("2026-05-01T12:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private ResendVerificationEmailService service;

    @BeforeEach
    void setUp() {
        TransactionTemplate tx = new TransactionTemplate() {
            @Override
            public <T> T execute(TransactionCallback<T> action) {
                return action.doInTransaction(mock(TransactionStatus.class));
            }
        };
        service = new ResendVerificationEmailService(
                new RateLimits(new MockEnvironment()),
                tokens,
                Runnable::run,
                repository,
                mail,
                tx,
                clock
        );
    }

    @Test
    void resendsVerificationWhenUnverifiedAndOutsideCooldown() {
        User unverified = User.createUnverified("ana@example.com", "hash", "Ana", "Lopez", null,
                Set.of(new Role((short) 1, RoleName.CUSTOMER)));

        when(repository.lockByEmail("ana@example.com")).thenReturn(Optional.of(unverified));
        when(repository.findTokenCreatedAt(unverified.getId()))
                .thenReturn(Optional.of(now.minusSeconds(65)));

        service.execute(new ResendVerificationCommand("ANA@EXAMPLE.COM"));

        verify(repository).replaceToken(eq(unverified.getId()), any(), eq(now.plusSeconds(86400)), eq(now));
        verify(mail).sendVerification(eq("ana@example.com"), eq("Ana"), any(), eq(now.plusSeconds(86400)));
    }

    @Test
    void suppressesResendWithinCooldownWindow() {
        User unverified = User.createUnverified("ana@example.com", "hash", "Ana", "Lopez", null,
                Set.of(new Role((short) 1, RoleName.CUSTOMER)));

        when(repository.lockByEmail("ana@example.com")).thenReturn(Optional.of(unverified));
        when(repository.findTokenCreatedAt(unverified.getId()))
                .thenReturn(Optional.of(now.minusSeconds(20)));

        service.execute(new ResendVerificationCommand("ana@example.com"));

        verify(repository, never()).replaceToken(any(), any(), any(), any());
        verifyNoInteractions(mail);
    }

    @Test
    void ignoresAlreadyVerifiedAndUnknownEmailsSilently() {
        User verified = User.create("verified@example.com", "hash", "Ana", "Lopez", null,
                Set.of(new Role((short) 1, RoleName.CUSTOMER)));

        when(repository.lockByEmail("verified@example.com")).thenReturn(Optional.of(verified));
        when(repository.lockByEmail("unknown@example.com")).thenReturn(Optional.empty());

        service.execute(new ResendVerificationCommand("verified@example.com"));
        service.execute(new ResendVerificationCommand("unknown@example.com"));

        verify(repository, never()).replaceToken(any(), any(), any(), any());
        verifyNoInteractions(mail);
    }
}

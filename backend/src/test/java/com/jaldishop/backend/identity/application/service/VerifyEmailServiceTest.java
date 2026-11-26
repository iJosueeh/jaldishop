package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.application.command.VerifyEmailCommand;
import com.jaldishop.backend.identity.application.port.EmailVerificationRepository;
import com.jaldishop.backend.identity.domain.enums.RoleName;
import com.jaldishop.backend.identity.domain.enums.UserStatus;
import com.jaldishop.backend.identity.domain.model.Role;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.infrastructure.security.OpaqueTokenCodec;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerifyEmailServiceTest {

    @Mock private EmailVerificationRepository repository;

    private final OpaqueTokenCodec tokens = new OpaqueTokenCodec();
    private final Instant now = Instant.parse("2026-05-01T12:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private VerifyEmailService service;

    @BeforeEach
    void setUp() {
        TransactionTemplate tx = new TransactionTemplate() {
            @Override
            public <T> T execute(TransactionCallback<T> action) {
                return action.doInTransaction(mock(TransactionStatus.class));
            }
        };
        service = new VerifyEmailService(repository, tokens, tx, clock);
    }

    @Test
    void verifiesActiveUserWithValidToken() {
        String rawToken = tokens.generate();
        String hash = tokens.hash(rawToken);
        User user = User.createUnverified("ana@example.com", "hash", "Ana", "Lopez", null,
                Set.of(new Role((short) 1, RoleName.CUSTOMER)));

        when(repository.findUserId(hash)).thenReturn(Optional.of(user.getId()));
        when(repository.lockById(user.getId())).thenReturn(Optional.of(user));
        when(repository.consume(user.getId(), hash, now)).thenReturn(true);

        service.execute(new VerifyEmailCommand(rawToken));

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getEmailVerifiedAt()).isEqualTo(now);
        verify(repository).markEmailVerified(user);
    }

    @Test
    void rejectsMalformedTokenWithoutQueryingDatabase() {
        assertThatThrownBy(() -> service.execute(new VerifyEmailCommand("invalid-token")))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo("INVALID_EMAIL_VERIFICATION_TOKEN");

        verifyNoInteractions(repository);
    }

    @Test
    void rejectsExpiredOrConsumedToken() {
        String rawToken = tokens.generate();
        String hash = tokens.hash(rawToken);
        User user = User.createUnverified("ana@example.com", "hash", "Ana", "Lopez", null,
                Set.of(new Role((short) 1, RoleName.CUSTOMER)));

        when(repository.findUserId(hash)).thenReturn(Optional.of(user.getId()));
        when(repository.lockById(user.getId())).thenReturn(Optional.of(user));
        when(repository.consume(user.getId(), hash, now)).thenReturn(false);

        assertThatThrownBy(() -> service.execute(new VerifyEmailCommand(rawToken)))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo("INVALID_EMAIL_VERIFICATION_TOKEN");

        verify(repository, never()).markEmailVerified(any());
    }

    @Test
    void rejectsSuspendedUserEvenWithMatchingToken() {
        String rawToken = tokens.generate();
        String hash = tokens.hash(rawToken);
        UUID id = UUID.randomUUID();
        User suspended = User.reconstitute(id, "ana@example.com", "hash", "Ana", "Lopez", null,
                UserStatus.SUSPENDED, Set.of(new Role((short) 1, RoleName.CUSTOMER)), now, now, false, false, null);

        when(repository.findUserId(hash)).thenReturn(Optional.of(id));
        when(repository.lockById(id)).thenReturn(Optional.of(suspended));

        assertThatThrownBy(() -> service.execute(new VerifyEmailCommand(rawToken)))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo("INVALID_EMAIL_VERIFICATION_TOKEN");

        verify(repository, never()).markEmailVerified(any());
    }
}

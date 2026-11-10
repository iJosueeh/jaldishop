package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.application.port.*;
import com.jaldishop.backend.identity.domain.model.*;
import com.jaldishop.backend.identity.domain.enums.*;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.identity.application.command.LoginCommand;
import com.jaldishop.backend.identity.infrastructure.security.JwtService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.*;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordRecoveryServiceTest {
    @Mock PasswordRecoveryRepository repository;
    @Mock PasswordResetMail mail;
    @Mock PlatformTransactionManager manager;
    PasswordRecoveryService service;
    PasswordResetTokens tokens = new PasswordResetTokens();
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    Instant now = Instant.parse("2026-11-10T12:00:00Z");
    User user;
    @BeforeEach void setup() {
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        service = new PasswordRecoveryService(repository, tokens, encoder, mail,
                new TransactionTemplate(manager), Clock.fixed(now, ZoneOffset.UTC));
        user = User.create("user@example.com", encoder.encode("old-password"), "Test", "User", null,
                Set.of(new Role((short) 1, RoleName.CUSTOMER)));
    }
    @Test void issuesRandomHashedTokenForExactlyFifteenMinutesAndSendsAfterCommit() {
        when(repository.lockByEmail("user@example.com")).thenReturn(Optional.of(user));
        service.issue(" USER@EXAMPLE.COM ");
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
        when(repository.lockByEmail("missing@example.com")).thenReturn(Optional.empty());
        user.suspend(null);
        when(repository.lockByEmail("user@example.com")).thenReturn(Optional.of(user));
        service.issue("missing@example.com"); service.issue("user@example.com");
        verifyNoInteractions(mail);
        verify(repository, never()).replaceToken(any(), any(), any());
    }
    @Test void consumesOnceEncodesPasswordAndIncrementsCredentialVersion() {
        String token = tokens.generate(); String hash = tokens.hash(token);
        when(repository.findUserId(hash)).thenReturn(Optional.of(user.getId()));
        when(repository.lockById(user.getId())).thenReturn(Optional.of(user));
        when(repository.consume(user.getId(), hash, now)).thenReturn(true);
        service.reset(token, "new-password");
        assertThat(encoder.matches("new-password", user.getPassword())).isTrue();
        assertThat(encoder.matches("old-password", user.getPassword())).isFalse();
        assertThat(user.getCredentialsVersion()).isEqualTo(1);
        verify(repository).updateCredentials(user);
        verify(manager).commit(any());
        var users = mock(UserRepository.class);
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        var jwt = new JwtService("local-test-signing-key-with-at-least-32-bytes", 600000);
        var login = new AuthenticateUserService(users, encoder, jwt);
        assertThatThrownBy(() -> login.execute(new LoginCommand(user.getEmail(), "old-password")))
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
        var loggedIn = login.execute(new LoginCommand(user.getEmail(), "new-password"));
        assertThat(jwt.getPrincipal(loggedIn.token()).credentialsVersion()).isEqualTo(1);
        assertThat(loggedIn.roles()).contains("CUSTOMER");
    }
    @Test void expiredOrConsumedTokenRollsBackAndKeepsCredentials() {
        String token = tokens.generate(); String hash = tokens.hash(token);
        when(repository.findUserId(hash)).thenReturn(Optional.of(user.getId()));
        when(repository.lockById(user.getId())).thenReturn(Optional.of(user));
        when(repository.consume(user.getId(), hash, now)).thenReturn(false);
        assertThatThrownBy(() -> service.reset(token, "new-password"))
                .hasMessage("El token de recuperación no es válido o ha vencido.");
        verify(manager).rollback(any()); verify(repository, never()).updateCredentials(any());
        assertThat(encoder.matches("old-password", user.getPassword())).isTrue();
    }
    @Test void nonexistentTokenHasSameError() {
        String token = tokens.generate();
        when(repository.findUserId(tokens.hash(token))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.reset(token, "new-password"))
                .hasMessage("El token de recuperación no es válido o ha vencido.");
        verify(manager).rollback(any());
    }
    @Test void credentialFailureRollsBackConsumption() {
        String token = tokens.generate(); String hash = tokens.hash(token);
        when(repository.findUserId(hash)).thenReturn(Optional.of(user.getId()));
        when(repository.lockById(user.getId())).thenReturn(Optional.of(user));
        when(repository.consume(user.getId(), hash, now)).thenReturn(true);
        doThrow(new IllegalStateException("database unavailable")).when(repository).updateCredentials(any());
        assertThatThrownBy(() -> service.reset(token, "new-password")).isInstanceOf(IllegalStateException.class);
        verify(manager).rollback(any());
    }
    @Test void rejectsMultibytePasswordsBeyondBcryptLimitBeforeDatabaseAccess() {
        Mockito.reset(manager);
        assertThatThrownBy(() -> service.reset(tokens.generate(), "é".repeat(37)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository, manager);
    }
}

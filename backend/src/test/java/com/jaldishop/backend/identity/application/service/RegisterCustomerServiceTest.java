package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.application.command.RegisterCustomerCommand;
import com.jaldishop.backend.identity.application.port.EmailVerificationMail;
import com.jaldishop.backend.identity.application.port.EmailVerificationRepository;
import com.jaldishop.backend.identity.domain.enums.RoleName;
import com.jaldishop.backend.identity.domain.model.Role;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.RoleRepository;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.identity.infrastructure.security.OpaqueTokenCodec;
import com.jaldishop.backend.shared.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterCustomerServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailVerificationRepository verificationRepository;
    @Mock private EmailVerificationMail verificationMail;

    private final OpaqueTokenCodec tokens = new OpaqueTokenCodec();
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-01T12:00:00Z"), ZoneOffset.UTC);

    private RegisterCustomerService service;

    @BeforeEach
    void setUp() {
        service = new RegisterCustomerService(
                userRepository,
                roleRepository,
                passwordEncoder,
                tokens,
                verificationRepository,
                verificationMail,
                Runnable::run,
                clock
        );
    }

    @Test
    void registersUnverifiedCustomerPersistsHashedTokenAndSendsMail() {
        Role customerRole = new Role((short) 1, RoleName.CUSTOMER);
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User created = service.execute(new RegisterCustomerCommand(
                "ana@example.com", "secret123", "Ana", "Lopez", "987654321"
        ));

        assertThat(created.isEmailVerified()).isFalse();
        assertThat(created.getEmailVerifiedAt()).isNull();
        assertThat(created.canUseAuthenticatedSession()).isFalse();

        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Instant> expiresCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(verificationRepository).replaceToken(
                eq(created.getId()),
                hashCaptor.capture(),
                expiresCaptor.capture(),
                eq(Instant.parse("2026-05-01T12:00:00Z"))
        );

        ArgumentCaptor<String> rawTokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(verificationMail).sendVerification(
                eq("ana@example.com"),
                eq("Ana"),
                rawTokenCaptor.capture(),
                eq(Instant.parse("2026-05-02T12:00:00Z"))
        );

        String rawToken = rawTokenCaptor.getValue();
        assertThat(tokens.isValidFormat(rawToken)).isTrue();
        assertThat(hashCaptor.getValue()).isEqualTo(tokens.hash(rawToken));
        assertThat(expiresCaptor.getValue()).isEqualTo(Instant.parse("2026-05-02T12:00:00Z"));
    }

    @Test
    void throwsConflictWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.execute(new RegisterCustomerCommand(
                "ana@example.com", "secret123", "Ana", "Lopez", null
        ))).isInstanceOf(ConflictException.class);

        verifyNoInteractions(verificationRepository, verificationMail);
    }

    @Test
    void doesNotFailRegistrationIfMailDeliveryThrows() {
        Role customerRole = new Role((short) 1, RoleName.CUSTOMER);
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new IllegalStateException("SMTP down"))
                .when(verificationMail).sendVerification(any(), any(), any(), any());

        User created = service.execute(new RegisterCustomerCommand(
                "ana@example.com", "secret123", "Ana", "Lopez", null
        ));

        assertThat(created.isEmailVerified()).isFalse();
        verify(verificationRepository).replaceToken(any(), any(), any(), any());
    }
}

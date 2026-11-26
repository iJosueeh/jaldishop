package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.application.command.LoginCommand;
import com.jaldishop.backend.identity.domain.enums.RoleName;
import com.jaldishop.backend.identity.domain.model.Role;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.identity.infrastructure.security.JwtService;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    private AuthenticateUserService service;

    @BeforeEach
    void setUp() {
        service = new AuthenticateUserService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void rejectsLoginWhenEmailIsNotVerified() {
        User unverified = User.createUnverified(
                "ana@example.com", "encoded", "Ana", "Lopez", null,
                Set.of(new Role((short) 1, RoleName.CUSTOMER))
        );
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(unverified));
        when(passwordEncoder.matches("secret123", "encoded")).thenReturn(true);

        assertThatThrownBy(() -> service.execute(new LoginCommand("ana@example.com", "secret123")))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo("EMAIL_NOT_VERIFIED");

        verifyNoInteractions(jwtService);
    }

    @Test
    void issuesTokenWhenEmailIsVerified() {
        User verified = User.create(
                "ana@example.com", "encoded", "Ana", "Lopez", null,
                Set.of(new Role((short) 1, RoleName.CUSTOMER))
        );
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(verified));
        when(passwordEncoder.matches("secret123", "encoded")).thenReturn(true);
        when(jwtService.generateToken(verified.getId(), Set.of("CUSTOMER"), 0L)).thenReturn("jwt-token");

        var result = service.execute(new LoginCommand("ana@example.com", "secret123"));

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.email()).isEqualTo("ana@example.com");
    }
}

package com.jaldishop.backend.identity.application;

import com.jaldishop.backend.identity.domain.User;
import com.jaldishop.backend.identity.domain.UserRepository;
import com.jaldishop.backend.identity.infrastructure.security.JwtService;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthenticateUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthenticateUserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResult execute(LoginCommand loginCommand) {
        User user = userRepository.findByEmail(loginCommand.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas."));

        if (!passwordEncoder.matches(loginCommand.password(), user.getPassword())) {
            throw new BadCredentialsException("Credenciales inválidas.");
        }

        if (!user.canAuthenticate()) {
            throw new BusinessRuleException("USER_INACTIVE", "El usuario se encuentra inactivo.");
        }

        Set<String> roles = user.getRoles().stream()
                .map(role -> role.getName().toString())
                .collect(Collectors.toSet());

        String token = jwtService.generateToken(user.getId(), roles);

        return new AuthResult(
                token,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                roles
        );
    }
}

package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.application.result.AuthResult;

import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.identity.infrastructure.security.JwtService;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RefreshTokenService {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public RefreshTokenService(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public AuthResult execute(UUID userId, long credentialsVersion) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));

        if (!user.canAuthenticate() || user.getCredentialsVersion() != credentialsVersion) {
            throw new org.springframework.security.authentication.BadCredentialsException("Sesión revocada.");
        }

        Set<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());

        String token = jwtService.generateToken(user.getId(), roleNames, user.getCredentialsVersion());

        return new AuthResult(
                token,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                roleNames
        );
    }
}

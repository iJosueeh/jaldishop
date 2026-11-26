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
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@Service
@Transactional
public class RegisterCustomerService {

    private static final Logger log = LoggerFactory.getLogger(RegisterCustomerService.class);
    private static final Duration VERIFICATION_TOKEN_TTL = Duration.ofHours(24);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final OpaqueTokenCodec tokens;
    private final EmailVerificationRepository verificationRepository;
    private final EmailVerificationMail verificationMail;
    private final TaskExecutor executor;
    private final Clock clock;

    public RegisterCustomerService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            OpaqueTokenCodec tokens,
            EmailVerificationRepository verificationRepository,
            EmailVerificationMail verificationMail,
            @Qualifier("passwordRecoveryExecutor") TaskExecutor executor,
            @Qualifier("passwordRecoveryClock") Clock clock
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.verificationRepository = verificationRepository;
        this.verificationMail = verificationMail;
        this.executor = executor;
        this.clock = clock;
    }

    public User execute(RegisterCustomerCommand registerCustomerCommand) {
        if (userRepository.existsByEmail(registerCustomerCommand.email())) {
            throw new ConflictException("EMAIL_ALREADY_EXISTS", "El correo ya esta registrado.");
        }

        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Rol CUSTOMER no encontrado."));

        String encodedPassword = passwordEncoder.encode(registerCustomerCommand.password());

        User user = User.createUnverified(
                registerCustomerCommand.email(),
                encodedPassword,
                registerCustomerCommand.firstName(),
                registerCustomerCommand.lastName(),
                registerCustomerCommand.phone(),
                Set.of(customerRole)
        );

        User saved = userRepository.save(user);

        String rawToken = tokens.generate();
        Instant now = clock.instant();
        Instant expiresAt = now.plus(VERIFICATION_TOKEN_TTL);
        verificationRepository.replaceToken(saved.getId(), tokens.hash(rawToken), expiresAt, now);

        scheduleVerificationMail(saved.getEmail(), saved.getFirstName(), rawToken, expiresAt);
        return saved;
    }

    private void scheduleVerificationMail(String recipientEmail, String firstName, String rawToken, Instant expiresAt) {
        Runnable dispatch = () -> executor.execute(() -> {
            try {
                verificationMail.sendVerification(recipientEmail, firstName, rawToken, expiresAt);
            } catch (RuntimeException failure) {
                log.warn("No se pudo enviar el correo de verificación tras el registro.");
            }
        });

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch.run();
                }
            });
        } else {
            dispatch.run();
        }
    }

}

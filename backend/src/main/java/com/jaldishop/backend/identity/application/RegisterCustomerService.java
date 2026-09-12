package com.jaldishop.backend.identity.application;

import com.jaldishop.backend.identity.domain.*;
import com.jaldishop.backend.identity.infrastructure.security.PasswordConfiguration;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Transactional
public class RegisterCustomerService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterCustomerService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User execute(RegisterCustomerCommand registerCustomerCommand) {
        if (userRepository.existsByEmail(registerCustomerCommand.email())) {
            throw new ConflictException("EMAIL_ALREADY_EXISTS", "El correo ya esta registrado.");
        }

        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Rol CUSTOMER no encontrado."));

        String encodedPassword = passwordEncoder.encode(registerCustomerCommand.password());

        User user = User.create(
                registerCustomerCommand.email(),
                encodedPassword,
                registerCustomerCommand.firstName(),
                registerCustomerCommand.lastName(),
                registerCustomerCommand.phone(),
                Set.of(customerRole)
        );

        return userRepository.save(user);
    }

}

package com.jaldishop.backend.identity.application;

import com.jaldishop.backend.identity.domain.*;
import com.jaldishop.backend.identity.infrastructure.security.JwtService;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.application.CreateStoreCommand;
import com.jaldishop.backend.store.application.CreateStoreService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class RegisterMerchantService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CreateStoreService createStoreService;
    private final JwtService jwtService;

    public RegisterMerchantService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, CreateStoreService createStoreService, JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.createStoreService = createStoreService;
        this.jwtService = jwtService;
    }

    public AuthResult execute(RegisterMerchantCommand command) {
        Role merchantRole = roleRepository.findByName(RoleName.MERCHANT)
                .orElseThrow(() -> new ResourceNotFoundException("Rol MERCHANT no encontrado."));

        Optional<User> existingUserOpt = userRepository.findByEmail(command.email());
        User user;

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();

            if (existingUser.hasRole(RoleName.MERCHANT)) {
                throw new ConflictException(
                        "MERCHANT_ALREADY_EXISTS",
                        "El correo ya está registrado como comerciante. Inicia sesión para acceder a tu panel."
                );
            }

            if (!passwordEncoder.matches(command.password(), existingUser.getPassword())) {
                throw new BadCredentialsException(
                        "Este correo ya esta registrado como cliente. La contraseña ingresada no coincide con tu cuenta."
                );
            }

            existingUser.addRole(merchantRole);
            user = userRepository.save(existingUser);
        } else {
            Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                    .orElseThrow(() -> new ResourceNotFoundException("Rol CUSTOMER no encontrado."));

            String encodedPassword = passwordEncoder.encode(command.password());
            User newUser = User.create(
                    command.email(),
                    encodedPassword,
                    command.firstName(),
                    command.lastName(),
                    command.phone(),
                    Set.of(customerRole, merchantRole)
            );
            user = userRepository.save(newUser);
        }

        String contactPhone = (command.storeContactPhone() != null && !command.storeContactPhone().isBlank())
                ? command.storeContactPhone()
                : command.phone();

        CreateStoreCommand storeCommand = new CreateStoreCommand(
                user.getId(),
                command.storeName(),
                null,
                command.businessType(),
                contactPhone,
                command.address(),
                null,
                null,
                null,
                command.pickupEnabled(),
                command.deliveryEnabled(),
                null,
                null,
                false,
                null
        );

        createStoreService.execute(storeCommand);

        Set<String> roleName = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());

        String token = jwtService.generateToken(user.getId(), roleName);

        return new AuthResult(
                token,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                roleName
        );
    }

}

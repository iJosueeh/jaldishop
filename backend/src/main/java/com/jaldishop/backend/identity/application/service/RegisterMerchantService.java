package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.application.command.RegisterMerchantCommand;
import com.jaldishop.backend.identity.application.port.StoreProvisioning;
import com.jaldishop.backend.identity.application.port.StoreProvisioningData;
import com.jaldishop.backend.identity.application.result.AuthResult;
import com.jaldishop.backend.identity.domain.enums.RoleName;
import com.jaldishop.backend.identity.domain.model.Role;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.RoleRepository;
import com.jaldishop.backend.identity.domain.repository.UserRepository;

import com.jaldishop.backend.identity.infrastructure.security.JwtService;
import com.jaldishop.backend.identity.application.port.PlatformSettingsProvider;
import com.jaldishop.backend.identity.domain.model.PlatformSettings;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class RegisterMerchantService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final StoreProvisioning storeProvisioning;
    private final JwtService jwtService;
    private final PlatformSettingsProvider platformSettingsProvider;

    @Autowired
    public RegisterMerchantService(UserRepository userRepository,
                                   RoleRepository roleRepository,
                                   PasswordEncoder passwordEncoder,
                                   StoreProvisioning storeProvisioning,
                                   JwtService jwtService,
                                   PlatformSettingsProvider platformSettingsProvider) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.storeProvisioning = storeProvisioning;
        this.jwtService = jwtService;
        this.platformSettingsProvider = platformSettingsProvider;
    }

    public RegisterMerchantService(UserRepository userRepository,
                                   RoleRepository roleRepository,
                                   PasswordEncoder passwordEncoder,
                                   StoreProvisioning storeProvisioning,
                                   JwtService jwtService) {
        this(userRepository, roleRepository, passwordEncoder, storeProvisioning, jwtService, null);
    }

    public AuthResult execute(RegisterMerchantCommand command) {
        return execute(command, false);
    }

    public AuthResult execute(RegisterMerchantCommand command, boolean bypassRegistrationCheck) {
        if (!bypassRegistrationCheck && !command.sunatTermsAccepted()) {
            throw new BusinessRuleException(
                    "SUNAT_TERMS_REQUIRED",
                    "Debes aceptar los Términos de Uso y la Declaración de Datos Tributarios SUNAT para registrar tu negocio."
            );
        }

        if (!bypassRegistrationCheck && platformSettingsProvider != null) {
            PlatformSettings settings = platformSettingsProvider.getSettings();
            if (settings != null && !settings.allowMerchantRegistration()) {
                throw new BusinessRuleException(
                        "MERCHANT_REGISTRATION_DISABLED",
                        "El registro de comerciantes está temporalmente deshabilitado por la administración de la plataforma."
                );
            }
        }

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

            if (!existingUser.isEmailVerified()) {
                throw new BusinessRuleException(
                        "EMAIL_NOT_VERIFIED",
                        "Debes verificar tu correo electrónico antes de continuar."
                );
            }

            // No persistimos merchantRole aquí (tendría store_id=NULL, violando el CHECK de base de datos).
            // Se asignará contextualmente con el store_id generado dentro de createStoreService.execute(...).
            user = existingUser;
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
                    Set.of(customerRole)
            );
            user = userRepository.save(newUser);
        }

        String contactPhone = (command.storeContactPhone() != null && !command.storeContactPhone().isBlank())
                ? command.storeContactPhone()
                : command.phone();

        BigDecimal effectiveTaxRate = resolveInitialTaxRate(command.taxRegime(), command.taxRate());
        Instant termsAcceptedAt = command.sunatTermsAccepted() ? Instant.now() : null;

        StoreProvisioningData provisioningData = new StoreProvisioningData(
                user.getId(),
                command.storeName(),
                null,
                command.businessType(),
                contactPhone,
                command.address(),
                command.addressReference(),
                command.latitude(),
                command.longitude(),
                command.pickupEnabled(),
                command.deliveryEnabled(),
                null,
                "PEN",
                effectiveTaxRate,
                command.logoUrl(),
                command.bannerUrl(),
                null,
                null,
                contactPhone,
                command.categoryIds(),
                command.legalBusinessName(),
                command.taxIdType(),
                command.taxIdNumber(),
                command.taxRegime(),
                command.canIssueInvoices(),
                termsAcceptedAt
        );

        storeProvisioning.provisionStore(provisioningData);
        user.addRole(merchantRole);

        Set<String> roleName = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());

        String token = jwtService.generateToken(user.getId(), roleName, user.getCredentialsVersion());

        return new AuthResult(
                token,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                roleName
        );
    }

    private BigDecimal resolveInitialTaxRate(String taxRegime, BigDecimal requestedTaxRate) {
        if ("RUS".equalsIgnoreCase(taxRegime) || "NONE".equalsIgnoreCase(taxRegime)) {
            return BigDecimal.ZERO.setScale(2);
        }
        return requestedTaxRate != null ? requestedTaxRate : new BigDecimal("18.00");
    }

}

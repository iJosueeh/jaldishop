package com.jaldishop.backend.identity.application;

import com.jaldishop.backend.identity.domain.Role;
import com.jaldishop.backend.identity.domain.RoleName;
import com.jaldishop.backend.identity.domain.RoleRepository;
import com.jaldishop.backend.identity.domain.User;
import com.jaldishop.backend.identity.domain.UserRepository;
import com.jaldishop.backend.identity.infrastructure.security.JwtService;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.store.application.CreateStoreCommand;
import com.jaldishop.backend.store.application.CreateStoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterMerchantServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CreateStoreService createStoreService;

    @Mock
    private JwtService jwtService;

    private RegisterMerchantService registerMerchantService;

    private final Role customerRole = new Role((short) 1, RoleName.CUSTOMER);
    private final Role merchantRole = new Role((short) 2, RoleName.MERCHANT);

    @BeforeEach
    void setUp() {
        registerMerchantService = new RegisterMerchantService(
                userRepository,
                roleRepository,
                passwordEncoder,
                createStoreService,
                jwtService
        );
    }

    @Test
    @DisplayName("Registrar nuevo comerciante exitosamente creando usuario y tienda")
    void registerNewMerchantSuccessfully() {
        RegisterMerchantCommand command = new RegisterMerchantCommand(
                "nuevo@comercio.com",
                "password123",
                "Carlos",
                "Mendoza",
                "987654321",
                "Pastelería Dulce",
                "Repostería",
                "987654321",
                "Av. Principal 123",
                true,
                true
        );

        when(roleRepository.findByName(RoleName.MERCHANT)).thenReturn(Optional.of(merchantRole));
        when(userRepository.findByEmail("nuevo@comercio.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName(RoleName.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(), anySet())).thenReturn("mocked-jwt-token");

        AuthResult result = registerMerchantService.execute(command);

        assertNotNull(result);
        assertEquals("mocked-jwt-token", result.token());
        assertEquals("nuevo@comercio.com", result.email());
        assertEquals("Carlos Mendoza", result.fullName());
        assertTrue(result.roles().contains("MERCHANT"));
        assertTrue(result.roles().contains("CUSTOMER"));

        verify(userRepository).save(any(User.class));
        verify(createStoreService).execute(any(CreateStoreCommand.class));
        verify(jwtService).generateToken(any(), anySet());
    }

    @Test
    @DisplayName("Upgrade exitoso de cliente existente a comerciante con contraseña correcta")
    void upgradeExistingCustomerToMerchantSuccessfully() {
        User existingCustomer = User.create(
                "cliente@test.com",
                "hashed-password",
                "Ana",
                "Torres",
                "912345678",
                Set.of(customerRole)
        );

        RegisterMerchantCommand command = new RegisterMerchantCommand(
                "cliente@test.com",
                "password123",
                "Ana",
                "Torres",
                "912345678",
                "Cafetería Express",
                "Cafetería",
                "912345678",
                "Calle Central 45",
                true,
                false
        );

        when(roleRepository.findByName(RoleName.MERCHANT)).thenReturn(Optional.of(merchantRole));
        when(userRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(existingCustomer));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(), anySet())).thenReturn("upgrade-jwt-token");

        AuthResult result = registerMerchantService.execute(command);

        assertNotNull(result);
        assertEquals("upgrade-jwt-token", result.token());
        assertEquals("cliente@test.com", result.email());
        assertTrue(result.roles().contains("MERCHANT"));
        assertTrue(result.roles().contains("CUSTOMER"));

        assertTrue(existingCustomer.hasRole(RoleName.MERCHANT));
        verify(userRepository).save(existingCustomer);
        verify(createStoreService).execute(any(CreateStoreCommand.class));
    }

    @Test
    @DisplayName("Lanzar excepción cuando el correo ya tiene el rol MERCHANT")
    void throwExceptionWhenExistingUserIsAlreadyMerchant() {
        User existingMerchant = User.create(
                "comerciante@test.com",
                "hashed-password",
                "Luis",
                "Rojas",
                "999888777",
                Set.of(customerRole, merchantRole)
        );

        RegisterMerchantCommand command = new RegisterMerchantCommand(
                "comerciante@test.com",
                "password123",
                "Luis",
                "Rojas",
                "999888777",
                "Segunda Tienda",
                "Comercio",
                null,
                null,
                true,
                true
        );

        when(roleRepository.findByName(RoleName.MERCHANT)).thenReturn(Optional.of(merchantRole));
        when(userRepository.findByEmail("comerciante@test.com")).thenReturn(Optional.of(existingMerchant));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> registerMerchantService.execute(command)
        );

        assertEquals("MERCHANT_ALREADY_EXISTS", exception.getCode());
        verify(userRepository, never()).save(any(User.class));
        verify(createStoreService, never()).execute(any(CreateStoreCommand.class));
    }

    @Test
    @DisplayName("Lanzar BadCredentialsException cuando la contraseña de cliente existente no coincide")
    void throwExceptionWhenExistingCustomerPasswordDoesNotMatch() {
        User existingCustomer = User.create(
                "cliente@test.com",
                "hashed-password",
                "Ana",
                "Torres",
                "912345678",
                Set.of(customerRole)
        );

        RegisterMerchantCommand command = new RegisterMerchantCommand(
                "cliente@test.com",
                "wrong-password",
                "Ana",
                "Torres",
                "912345678",
                "Café Ana",
                "Cafetería",
                null,
                null,
                true,
                true
        );

        when(roleRepository.findByName(RoleName.MERCHANT)).thenReturn(Optional.of(merchantRole));
        when(userRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(existingCustomer));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> registerMerchantService.execute(command)
        );

        assertTrue(exception.getMessage().contains("contraseña"));
        verify(userRepository, never()).save(any(User.class));
        verify(createStoreService, never()).execute(any(CreateStoreCommand.class));
    }

    @Test
    @DisplayName("Propagar excepción cuando CreateStoreService falla")
    void throwExceptionWhenCreateStoreFails() {
        RegisterMerchantCommand command = new RegisterMerchantCommand(
                "nuevo@comercio.com",
                "password123",
                "Carlos",
                "Mendoza",
                "987654321",
                "Pastelería Dulce",
                "Repostería",
                null,
                null,
                true,
                true
        );

        when(roleRepository.findByName(RoleName.MERCHANT)).thenReturn(Optional.of(merchantRole));
        when(userRepository.findByEmail("nuevo@comercio.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName(RoleName.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(createStoreService.execute(any(CreateStoreCommand.class)))
                .thenThrow(new ConflictException("STORE_SLUG_ALREADY_EXISTS", "El slug ya existe"));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> registerMerchantService.execute(command)
        );

        assertEquals("STORE_SLUG_ALREADY_EXISTS", exception.getCode());
        verify(jwtService, never()).generateToken(any(), anySet());
    }
}

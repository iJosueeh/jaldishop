package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.domain.enums.RoleName;
import com.jaldishop.backend.identity.domain.enums.UserStatus;
import com.jaldishop.backend.identity.domain.model.Role;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.RoleRepository;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromoteUserToAdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private AdminAuditService auditService;

    @InjectMocks
    private PromoteUserToAdminService promoteUserToAdminService;

    private UUID primaryAdminId;
    private UUID operationalAdminId;
    private UUID customerUserId;
    private User primaryAdmin;
    private User operationalAdmin;
    private User customerUser;
    private Role adminRole;
    private Role customerRole;
    private Role merchantRole;

    @BeforeEach
    void setUp() {
        primaryAdminId = UUID.randomUUID();
        operationalAdminId = UUID.randomUUID();
        customerUserId = UUID.randomUUID();

        customerRole = new Role((short) 1, RoleName.CUSTOMER);
        merchantRole = new Role((short) 2, RoleName.MERCHANT);
        adminRole = new Role((short) 3, RoleName.ADMIN);

        primaryAdmin = User.reconstitute(
                primaryAdminId,
                "primary@jaldishop.com",
                "hashed",
                "Primary",
                "Admin",
                "987654321",
                UserStatus.ACTIVE,
                Set.of(adminRole),
                Instant.now(),
                Instant.now(),
                true
        );

        operationalAdmin = User.reconstitute(
                operationalAdminId,
                "op@jaldishop.com",
                "hashed",
                "Op",
                "Admin",
                "987654321",
                UserStatus.ACTIVE,
                Set.of(adminRole),
                Instant.now(),
                Instant.now(),
                false
        );

        customerUser = User.reconstitute(
                customerUserId,
                "customer@jaldishop.com",
                "hashed",
                "Customer",
                "User",
                "987654321",
                UserStatus.ACTIVE,
                Set.of(customerRole),
                Instant.now(),
                Instant.now(),
                false
        );
    }

    @Test
    @DisplayName("Administrador principal promueve exitosamente usuario CUSTOMER conservando roles e invalidando sesiones anteriores")
    void shouldPromoteCustomerUserToAdmin() {
        when(userRepository.findById(primaryAdminId)).thenReturn(Optional.of(primaryAdmin));
        when(userRepository.findById(customerUserId)).thenReturn(Optional.of(customerUser));
        when(storeRepository.findByOwnerUserId(customerUserId)).thenReturn(Optional.empty());
        when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.of(adminRole));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = promoteUserToAdminService.execute(customerUserId, primaryAdminId);

        assertThat(result.hasRole(RoleName.ADMIN)).isTrue();
        assertThat(result.hasRole(RoleName.CUSTOMER)).isTrue(); // Conserva CUSTOMER
        assertThat(result.isPrimaryAdmin()).isFalse(); // No es principal
        verify(userRepository).incrementCredentialsVersion(customerUserId); // Invalida sesiones activas
        verify(auditService).logSuccess(eq(primaryAdminId), any(), eq("PROMOTE_TO_ADMIN"), eq("USER"), eq(customerUserId), any(), any());
    }

    @Test
    @DisplayName("Seguridad: Administrador operativo no puede promover a ningún usuario")
    void shouldRejectOperationalAdminFromPromoting() {
        when(userRepository.findById(operationalAdminId)).thenReturn(Optional.of(operationalAdmin));

        assertThatThrownBy(() -> promoteUserToAdminService.execute(customerUserId, operationalAdminId))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Solo el administrador principal puede promover usuarios");
    }

    @Test
    @DisplayName("Seguridad: Prohíbe la autopromoción")
    void shouldRejectSelfPromotion() {
        when(userRepository.findById(primaryAdminId)).thenReturn(Optional.of(primaryAdmin));

        assertThatThrownBy(() -> promoteUserToAdminService.execute(primaryAdminId, primaryAdminId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("No puedes autopromoverte");
    }

    @Test
    @DisplayName("Seguridad: Bloquea la promoción directa de comerciantes con tiendas activas")
    void shouldRejectPromotingMerchantWithActiveStore() {
        User merchantUser = User.reconstitute(
                customerUserId,
                "merchant@jaldishop.com",
                "hashed",
                "Merchant",
                "Owner",
                "987654321",
                UserStatus.ACTIVE,
                Set.of(merchantRole),
                Instant.now(),
                Instant.now(),
                false
        );

        when(userRepository.findById(primaryAdminId)).thenReturn(Optional.of(primaryAdmin));
        when(userRepository.findById(customerUserId)).thenReturn(Optional.of(merchantUser));

        assertThatThrownBy(() -> promoteUserToAdminService.execute(customerUserId, primaryAdminId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("No se permite la promoción directa de comerciantes con tiendas activas");
    }

    @Test
    @DisplayName("Rechaza promover usuario que ya es ADMIN")
    void shouldRejectWhenUserAlreadyAdmin() {
        User existingAdmin = User.reconstitute(
                customerUserId,
                "existing.admin@jaldishop.com",
                "hashed",
                "Existing",
                "Admin",
                "987654321",
                UserStatus.ACTIVE,
                Set.of(adminRole),
                Instant.now(),
                Instant.now(),
                false
        );

        when(userRepository.findById(primaryAdminId)).thenReturn(Optional.of(primaryAdmin));
        when(userRepository.findById(customerUserId)).thenReturn(Optional.of(existingAdmin));

        assertThatThrownBy(() -> promoteUserToAdminService.execute(customerUserId, primaryAdminId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ya cuenta con el rol ADMIN");
    }
}

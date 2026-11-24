package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.domain.enums.UserStatus;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.shared.exception.ResourceConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminDeleteUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminAuditService auditService;

    private AdminDeleteUserService service;
    private UUID adminId;
    private UUID targetId;

    @BeforeEach
    void setUp() {
        service = new AdminDeleteUserService(userRepository, auditService);
        adminId = UUID.randomUUID();
        targetId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Bloquea auto-eliminación con 409 CANNOT_DELETE_SELF")
    void blockSelfDeletion() {
        ResourceConflictException ex = assertThrows(ResourceConflictException.class, () -> service.execute(adminId, adminId));
        assertEquals("CANNOT_DELETE_SELF", ex.getCode());
    }

    @Test
    @DisplayName("Bloquea eliminación de administrador principal con 403 AccessDeniedException")
    void blockPrimaryAdminDeletion() {
        User primaryAdmin = User.reconstitute(targetId, "admin@jaldi.com", "hash", "Main", "Admin", null, UserStatus.ACTIVE, Set.of(), Instant.now(), Instant.now(), true);
        when(userRepository.lockByIdForDeletion(targetId)).thenReturn(Optional.of(primaryAdmin));

        assertThrows(AccessDeniedException.class, () -> service.execute(targetId, adminId));
        verify(userRepository, never()).deleteUserAndSafeOrphans(any());
    }

    @Test
    @DisplayName("Bloquea eliminación de usuario con historial operativo o de auditoría (409)")
    void blockUserWithOperationalHistory() {
        User user = User.reconstitute(targetId, "user@jaldi.com", "hash", "Test", "User", null, UserStatus.ACTIVE, Set.of(), Instant.now(), Instant.now(), false);
        when(userRepository.lockByIdForDeletion(targetId)).thenReturn(Optional.of(user));
        when(userRepository.hasOperationalOrAuditActorHistory(targetId)).thenReturn(true);

        ResourceConflictException ex = assertThrows(ResourceConflictException.class, () -> service.execute(targetId, adminId));

        assertEquals("USER_HAS_OPERATIONAL_HISTORY", ex.getCode());
        assertEquals("SUSPEND", ex.getMetadata().get("suggestedAction"));
        verify(userRepository, never()).deleteUserAndSafeOrphans(any());
    }

    @Test
    @DisplayName("Elimina usuario creado por error sin historial y registra snapshot USER_DELETED")
    void deleteCleanUserAndLogSnapshot() {
        User user = User.reconstitute(targetId, "typo@jaldi.com", "hash", "Typo", "User", null, UserStatus.ACTIVE, Set.of(), Instant.now(), Instant.now(), false);
        when(userRepository.lockByIdForDeletion(targetId)).thenReturn(Optional.of(user));
        when(userRepository.hasOperationalOrAuditActorHistory(targetId)).thenReturn(false);

        service.execute(targetId, adminId);

        verify(userRepository).deleteUserAndSafeOrphans(targetId);
        verify(auditService).logSuccess(eq(adminId), any(), eq("USER_DELETED"), eq("USER"), eq(targetId), isNull(), contains("typo@jaldi.com"));
    }
}

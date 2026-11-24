package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.shared.exception.ResourceConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class AdminDeleteUserService {

    private final UserRepository userRepository;
    private final AdminAuditService auditService;

    public AdminDeleteUserService(UserRepository userRepository) {
        this(userRepository, null);
    }

    @Autowired
    public AdminDeleteUserService(UserRepository userRepository, AdminAuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    public void execute(UUID targetUserId, UUID actorAdminId) {
        if (actorAdminId != null && actorAdminId.equals(targetUserId)) {
            throw new ResourceConflictException(
                    "CANNOT_DELETE_SELF",
                    "No puedes eliminar tu propia cuenta de administrador.",
                    Map.of("blockingReason", "SELF_DELETION")
            );
        }

        User target = userRepository.lockByIdForDeletion(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", targetUserId.toString()));

        if (target.isPrimaryAdmin()) {
            throw new AccessDeniedException("No es posible eliminar la cuenta del administrador principal.");
        }

        if (userRepository.hasOperationalOrAuditActorHistory(targetUserId)) {
            throw new ResourceConflictException(
                    "USER_HAS_OPERATIONAL_HISTORY",
                    "Este usuario tiene tienda, pedidos, reseñas, reservas o acciones administrativas registradas y no puede eliminarse permanentemente. Puedes suspender la cuenta en su lugar.",
                    Map.of(
                            "suggestedAction", "SUSPEND",
                            "currentStatus", target.getStatus().name(),
                            "blockingReason", "OPERATIONAL_HISTORY"
                    )
            );
        }

        String snapshotEmail = target.getEmail();
        String snapshotName = target.getFullName();

        userRepository.deleteUserAndSafeOrphans(targetUserId);

        if (auditService != null) {
            String adminEmail = actorAdminId != null
                    ? userRepository.findById(actorAdminId).map(User::getEmail).orElse(null)
                    : null;
            auditService.logSuccess(
                    actorAdminId,
                    adminEmail,
                    "USER_DELETED",
                    "USER",
                    targetUserId,
                    null,
                    "Usuario eliminado permanentemente: " + snapshotName + " (" + snapshotEmail + ")"
            );
        }
    }
}

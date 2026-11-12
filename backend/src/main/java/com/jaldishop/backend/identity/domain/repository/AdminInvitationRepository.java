package com.jaldishop.backend.identity.domain.repository;

import com.jaldishop.backend.identity.domain.model.AdminInvitation;

import java.util.Optional;
import java.util.UUID;

public interface AdminInvitationRepository {
    AdminInvitation save(AdminInvitation invitation);
    Optional<AdminInvitation> findById(UUID id);
    Optional<AdminInvitation> findByTokenHash(String tokenHash);
    Optional<AdminInvitation> findByTokenHashForUpdate(String tokenHash);
    Optional<AdminInvitation> findPendingByEmail(String email);
    boolean existsPendingByEmail(String email);
    org.springframework.data.domain.Page<AdminInvitation> findInvitations(com.jaldishop.backend.identity.domain.enums.AdminInvitationStatus status, org.springframework.data.domain.Pageable pageable);
}

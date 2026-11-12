package com.jaldishop.backend.identity.infrastructure.persistence.adapter;

import com.jaldishop.backend.identity.domain.enums.AdminInvitationStatus;
import com.jaldishop.backend.identity.domain.model.AdminInvitation;
import com.jaldishop.backend.identity.domain.repository.AdminInvitationRepository;
import com.jaldishop.backend.identity.infrastructure.persistence.entity.AdminInvitationEntity;
import com.jaldishop.backend.identity.infrastructure.persistence.repository.AdminInvitationJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class AdminInvitationRepositoryAdapter implements AdminInvitationRepository {

    private final AdminInvitationJpaRepository jpaRepository;

    public AdminInvitationRepositoryAdapter(AdminInvitationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AdminInvitation save(AdminInvitation domain) {
        AdminInvitationEntity entity = new AdminInvitationEntity(
                domain.getId(),
                domain.getEmail(),
                domain.getTokenHash(),
                domain.getFirstName(),
                domain.getLastName(),
                domain.getPhone(),
                domain.getInvitedByUserId(),
                domain.getStatus(),
                domain.getExpiresAt(),
                domain.getCreatedAt(),
                domain.getAcceptedAt(),
                domain.getRevokedAt()
        );
        AdminInvitationEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<AdminInvitation> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<AdminInvitation> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    public Optional<AdminInvitation> findByTokenHashForUpdate(String tokenHash) {
        return jpaRepository.findByTokenHashForUpdate(tokenHash).map(this::toDomain);
    }

    @Override
    public Optional<AdminInvitation> findPendingByEmail(String email) {
        return jpaRepository.findByEmailAndStatus(email, AdminInvitationStatus.PENDING).map(this::toDomain);
    }

    @Override
    public boolean existsPendingByEmail(String email) {
        return jpaRepository.existsPendingByEmail(email);
    }

    @Override
    public org.springframework.data.domain.Page<AdminInvitation> findInvitations(
            AdminInvitationStatus status,
            org.springframework.data.domain.Pageable pageable
    ) {
        org.springframework.data.domain.Page<AdminInvitationEntity> entities = (status != null)
                ? jpaRepository.findByStatus(status, pageable)
                : jpaRepository.findAll(pageable);
        return entities.map(this::toDomain);
    }

    private AdminInvitation toDomain(AdminInvitationEntity entity) {
        return AdminInvitation.reconstitute(
                entity.getId(),
                entity.getEmail(),
                entity.getTokenHash(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getPhone(),
                entity.getInvitedByUserId(),
                entity.getStatus(),
                entity.getExpiresAt(),
                entity.getCreatedAt(),
                entity.getAcceptedAt(),
                entity.getRevokedAt()
        );
    }
}

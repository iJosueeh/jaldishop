package com.jaldishop.backend.identity.infrastructure.persistence.adapter;

import com.jaldishop.backend.identity.domain.enums.AdminAuditStatus;
import com.jaldishop.backend.identity.domain.model.AdminAuditLog;
import com.jaldishop.backend.identity.domain.repository.AdminAuditLogRepository;
import com.jaldishop.backend.identity.infrastructure.persistence.entity.AdminAuditLogEntity;
import com.jaldishop.backend.identity.infrastructure.persistence.repository.AdminAuditLogJpaRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class AdminAuditLogRepositoryAdapter implements AdminAuditLogRepository {

    private final AdminAuditLogJpaRepository jpaRepository;

    public AdminAuditLogRepositoryAdapter(AdminAuditLogJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AdminAuditLog save(AdminAuditLog domain) {
        AdminAuditLogEntity entity = new AdminAuditLogEntity(
                domain.getId(),
                domain.getActorId(),
                domain.getActorEmail(),
                domain.getAction(),
                domain.getTargetType(),
                domain.getTargetId(),
                domain.getStatus(),
                domain.getIpAddress(),
                domain.getDetails(),
                domain.getCreatedAt()
        );
        AdminAuditLogEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Page<AdminAuditLog> searchLogs(
            String action,
            AdminAuditStatus status,
            UUID actorId,
            Instant startDate,
            Instant endDate,
            Pageable pageable
    ) {
        Specification<AdminAuditLogEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (action != null && !action.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("action")), action.trim().toUpperCase()));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (actorId != null) {
                predicates.add(cb.equal(root.get("actorId"), actorId));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return jpaRepository.findAll(spec, pageable).map(this::toDomain);
    }

    private AdminAuditLog toDomain(AdminAuditLogEntity saved) {
        return new AdminAuditLog(
                saved.getId(),
                saved.getActorId(),
                saved.getActorEmail(),
                saved.getAction(),
                saved.getTargetType(),
                saved.getTargetId(),
                saved.getStatus(),
                saved.getIpAddress(),
                saved.getDetails(),
                saved.getCreatedAt()
        );
    }
}

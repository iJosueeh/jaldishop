package com.jaldishop.backend.identity.application.service;

import com.jaldishop.backend.identity.domain.enums.AdminInvitationStatus;
import com.jaldishop.backend.identity.domain.model.AdminInvitation;
import com.jaldishop.backend.identity.domain.repository.AdminInvitationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetAdminInvitationsService {

    private final AdminInvitationRepository invitationRepository;

    public GetAdminInvitationsService(AdminInvitationRepository invitationRepository) {
        this.invitationRepository = invitationRepository;
    }

    public Page<AdminInvitation> execute(AdminInvitationStatus status, int page, int size) {
        int validatedPage = Math.max(page, 0);
        int validatedSize = (size <= 0 || size > 100) ? 20 : size;

        Pageable pageable = PageRequest.of(validatedPage, validatedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return invitationRepository.findInvitations(status, pageable);
    }
}

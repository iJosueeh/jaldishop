package com.jaldishop.backend.identity.web.controller;

import com.jaldishop.backend.identity.application.command.InviteAdminCommand;
import com.jaldishop.backend.identity.application.service.InviteAdminService;
import com.jaldishop.backend.identity.application.service.PromoteUserToAdminService;
import com.jaldishop.backend.identity.application.service.RevokeAdminInvitationService;
import com.jaldishop.backend.identity.application.service.RevokeAdminService;
import com.jaldishop.backend.identity.domain.model.AdminInvitation;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.web.dto.AdminInvitationResponse;
import com.jaldishop.backend.identity.web.dto.AdminUserDetailResponse;
import com.jaldishop.backend.identity.web.dto.InviteAdminRequest;
import com.jaldishop.backend.identity.web.mapper.AdminUserResponseMapper;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/administrators")
@PreAuthorize("hasRole('ADMIN') and principal.primaryAdmin")
@Validated
public class AdminAdministratorController {

    private final InviteAdminService inviteAdminService;
    private final RevokeAdminInvitationService revokeAdminInvitationService;
    private final PromoteUserToAdminService promoteUserToAdminService;
    private final RevokeAdminService revokeAdminService;
    private final com.jaldishop.backend.identity.application.service.GetAdminInvitationsService getAdminInvitationsService;
    private final AdminUserResponseMapper responseMapper;

    public AdminAdministratorController(
            InviteAdminService inviteAdminService,
            RevokeAdminInvitationService revokeAdminInvitationService,
            PromoteUserToAdminService promoteUserToAdminService,
            RevokeAdminService revokeAdminService,
            AdminUserResponseMapper responseMapper
    ) {
        this(inviteAdminService, revokeAdminInvitationService, promoteUserToAdminService, revokeAdminService, null, responseMapper);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AdminAdministratorController(
            InviteAdminService inviteAdminService,
            RevokeAdminInvitationService revokeAdminInvitationService,
            PromoteUserToAdminService promoteUserToAdminService,
            RevokeAdminService revokeAdminService,
            com.jaldishop.backend.identity.application.service.GetAdminInvitationsService getAdminInvitationsService,
            AdminUserResponseMapper responseMapper
    ) {
        this.inviteAdminService = inviteAdminService;
        this.revokeAdminInvitationService = revokeAdminInvitationService;
        this.promoteUserToAdminService = promoteUserToAdminService;
        this.revokeAdminService = revokeAdminService;
        this.getAdminInvitationsService = getAdminInvitationsService;
        this.responseMapper = responseMapper;
    }

    @GetMapping("/invitations")
    public ResponseEntity<org.springframework.data.domain.Page<AdminInvitationResponse>> listInvitations(
            @RequestParam(required = false) com.jaldishop.backend.identity.domain.enums.AdminInvitationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (getAdminInvitationsService == null) {
            return ResponseEntity.ok(org.springframework.data.domain.Page.empty());
        }
        org.springframework.data.domain.Page<AdminInvitation> invitations = getAdminInvitationsService.execute(status, page, size);
        return ResponseEntity.ok(invitations.map(invitation -> new AdminInvitationResponse(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getFirstName(),
                invitation.getLastName(),
                invitation.getPhone(),
                invitation.getStatus(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt()
        )));
    }

    @PostMapping("/invitations")
    public ResponseEntity<AdminInvitationResponse> inviteAdmin(
            @Valid @RequestBody InviteAdminRequest request,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        UUID adminId = principal != null ? principal.userId() : null;
        InviteAdminCommand command = new InviteAdminCommand(
                request.email(),
                request.firstName(),
                request.lastName(),
                request.phone(),
                adminId
        );

        AdminInvitation invitation = inviteAdminService.execute(command);

        AdminInvitationResponse response = new AdminInvitationResponse(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getFirstName(),
                invitation.getLastName(),
                invitation.getPhone(),
                invitation.getStatus(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/invitations/{id}/revoke")
    public ResponseEntity<Void> revokeInvitation(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        UUID adminId = principal != null ? principal.userId() : null;
        revokeAdminInvitationService.execute(id, adminId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/promote")
    public ResponseEntity<AdminUserDetailResponse> promoteToAdmin(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        UUID adminId = principal != null ? principal.userId() : null;
        User user = promoteUserToAdminService.execute(id, adminId);
        return ResponseEntity.ok(responseMapper.toDetail(user));
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<AdminUserDetailResponse> revokeAdmin(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        UUID adminId = principal != null ? principal.userId() : null;
        User user = revokeAdminService.execute(id, adminId);
        return ResponseEntity.ok(responseMapper.toDetail(user));
    }
}

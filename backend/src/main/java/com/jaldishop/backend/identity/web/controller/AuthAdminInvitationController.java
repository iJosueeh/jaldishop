package com.jaldishop.backend.identity.web.controller;

import com.jaldishop.backend.identity.application.command.AcceptAdminInvitationCommand;
import com.jaldishop.backend.identity.application.service.AcceptAdminInvitationService;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.web.dto.AcceptAdminInvitationRequest;
import com.jaldishop.backend.identity.web.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/admin-invitations")
public class AuthAdminInvitationController {

    private final AcceptAdminInvitationService acceptAdminInvitationService;

    public AuthAdminInvitationController(AcceptAdminInvitationService acceptAdminInvitationService) {
        this.acceptAdminInvitationService = acceptAdminInvitationService;
    }

    @PostMapping("/accept")
    public ResponseEntity<UserResponse> acceptInvitation(@Valid @RequestBody AcceptAdminInvitationRequest request) {
        AcceptAdminInvitationCommand command = new AcceptAdminInvitationCommand(request.token(), request.newPassword());
        User user = acceptAdminInvitationService.execute(command);

        UserResponse response = new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getStatus(),
                user.getCreatedAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

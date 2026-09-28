package com.jaldishop.backend.identity.web.controller;

import com.jaldishop.backend.identity.application.ChangeUserStatusService;
import com.jaldishop.backend.identity.application.GetAdminUsersService;
import com.jaldishop.backend.identity.application.UpdateUserRolesService;
import com.jaldishop.backend.identity.domain.RoleName;
import com.jaldishop.backend.identity.domain.UserStatus;
import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import com.jaldishop.backend.identity.web.dto.AdminUserDetailResponse;
import com.jaldishop.backend.identity.web.dto.AdminUserSummaryResponse;
import com.jaldishop.backend.identity.web.dto.UpdateUserRolesRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final GetAdminUsersService getAdminUsersService;
    private final ChangeUserStatusService changeUserStatusService;
    private final UpdateUserRolesService updateUserRolesService;

    public AdminUserController(
            GetAdminUsersService getAdminUsersService,
            ChangeUserStatusService changeUserStatusService,
            UpdateUserRolesService updateUserRolesService
    ) {
        this.getAdminUsersService = getAdminUsersService;
        this.changeUserStatusService = changeUserStatusService;
        this.updateUserRolesService = updateUserRolesService;
    }

    @GetMapping
    public ResponseEntity<List<AdminUserSummaryResponse>> listUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) RoleName role,
            @RequestParam(required = false) UserStatus status
    ) {
        List<AdminUserSummaryResponse> users = getAdminUsersService.listUsers(query, role, status);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminUserDetailResponse> getUserById(@PathVariable UUID id) {
        AdminUserDetailResponse user = getAdminUsersService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @PatchMapping("/{id}/suspend")
    public ResponseEntity<AdminUserDetailResponse> suspendUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        UUID adminId = principal != null ? principal.userId() : null;
        AdminUserDetailResponse user = changeUserStatusService.suspend(id, adminId);
        return ResponseEntity.ok(user);
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<AdminUserDetailResponse> activateUser(@PathVariable UUID id) {
        AdminUserDetailResponse user = changeUserStatusService.activate(id);
        return ResponseEntity.ok(user);
    }

    @PatchMapping("/{id}/roles")
    public ResponseEntity<AdminUserDetailResponse> updateUserRoles(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateUserRolesRequest request,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        UUID adminId = principal != null ? principal.userId() : null;
        AdminUserDetailResponse user = updateUserRolesService.updateRoles(id, request.roles(), adminId);
        return ResponseEntity.ok(user);
    }
}


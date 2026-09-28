package com.jaldishop.backend.identity.web.controller;

import com.jaldishop.backend.identity.application.ChangeUserStatusCommand;
import com.jaldishop.backend.identity.application.ChangeUserStatusService;
import com.jaldishop.backend.identity.application.GetAdminUsersQuery;
import com.jaldishop.backend.identity.application.GetAdminUsersService;
import com.jaldishop.backend.identity.application.UpdateUserRolesService;
import com.jaldishop.backend.identity.domain.RoleName;
import com.jaldishop.backend.identity.domain.User;
import com.jaldishop.backend.identity.domain.UserStatus;
import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import com.jaldishop.backend.identity.web.dto.AdminUserDetailResponse;
import com.jaldishop.backend.identity.web.dto.AdminUserSummaryResponse;
import com.jaldishop.backend.identity.web.dto.UpdateUserRolesRequest;
import com.jaldishop.backend.identity.web.mapper.AdminUserResponseMapper;
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
    private final AdminUserResponseMapper responseMapper;

    public AdminUserController(
            GetAdminUsersService getAdminUsersService,
            ChangeUserStatusService changeUserStatusService,
            UpdateUserRolesService updateUserRolesService,
            AdminUserResponseMapper responseMapper
    ) {
        this.getAdminUsersService = getAdminUsersService;
        this.changeUserStatusService = changeUserStatusService;
        this.updateUserRolesService = updateUserRolesService;
        this.responseMapper = responseMapper;
    }

    @GetMapping
    public ResponseEntity<List<AdminUserSummaryResponse>> listUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) RoleName role,
            @RequestParam(required = false) UserStatus status
    ) {
        List<User> users = getAdminUsersService.execute(new GetAdminUsersQuery(query, role, status));
        List<AdminUserSummaryResponse> response = users.stream()
                .map(responseMapper::toSummary)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminUserDetailResponse> getUserById(@PathVariable UUID id) {
        User user = getAdminUsersService.execute(id);
        return ResponseEntity.ok(responseMapper.toDetail(user));
    }

    @PatchMapping("/{id}/suspend")
    public ResponseEntity<AdminUserDetailResponse> suspendUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        UUID adminId = principal != null ? principal.userId() : null;
        User user = changeUserStatusService.execute(new ChangeUserStatusCommand(id, UserStatus.SUSPENDED, adminId));
        return ResponseEntity.ok(responseMapper.toDetail(user));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<AdminUserDetailResponse> activateUser(@PathVariable UUID id) {
        User user = changeUserStatusService.execute(new ChangeUserStatusCommand(id, UserStatus.ACTIVE, null));
        return ResponseEntity.ok(responseMapper.toDetail(user));
    }

    @PatchMapping("/{id}/roles")
    public ResponseEntity<AdminUserDetailResponse> updateUserRoles(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateUserRolesRequest request,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        UUID adminId = principal != null ? principal.userId() : null;
        User user = updateUserRolesService.execute(id, request.roles(), adminId);
        return ResponseEntity.ok(responseMapper.toDetail(user));
    }
}

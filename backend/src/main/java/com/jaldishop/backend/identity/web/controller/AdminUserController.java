package com.jaldishop.backend.identity.web.controller;

import com.jaldishop.backend.identity.application.command.AdminCreateUserCommand;
import com.jaldishop.backend.identity.application.command.ChangeUserStatusCommand;
import com.jaldishop.backend.identity.application.command.RegisterMerchantCommand;
import com.jaldishop.backend.identity.application.query.GetAdminUsersQuery;
import com.jaldishop.backend.identity.application.result.AuthResult;
import com.jaldishop.backend.identity.application.service.*;
import com.jaldishop.backend.identity.domain.enums.RoleName;
import com.jaldishop.backend.identity.domain.enums.UserStatus;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.identity.web.dto.*;
import com.jaldishop.backend.identity.web.mapper.AdminUserResponseMapper;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminUserController {

    private final GetAdminUsersService getAdminUsersService;
    private final ChangeUserStatusService changeUserStatusService;
    private final UpdateUserRolesService updateUserRolesService;
    private final AdminCreateUserService adminCreateUserService;
    private final AdminCreateMerchantService adminCreateMerchantService;
    private final AdminUserResponseMapper responseMapper;

    public AdminUserController(
            GetAdminUsersService getAdminUsersService,
            ChangeUserStatusService changeUserStatusService,
            UpdateUserRolesService updateUserRolesService,
            AdminUserResponseMapper responseMapper
    ) {
        this(getAdminUsersService, changeUserStatusService, updateUserRolesService, null, null, responseMapper);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AdminUserController(
            GetAdminUsersService getAdminUsersService,
            ChangeUserStatusService changeUserStatusService,
            UpdateUserRolesService updateUserRolesService,
            AdminCreateUserService adminCreateUserService,
            AdminCreateMerchantService adminCreateMerchantService,
            AdminUserResponseMapper responseMapper
    ) {
        this.getAdminUsersService = getAdminUsersService;
        this.changeUserStatusService = changeUserStatusService;
        this.updateUserRolesService = updateUserRolesService;
        this.adminCreateUserService = adminCreateUserService;
        this.adminCreateMerchantService = adminCreateMerchantService;
        this.responseMapper = responseMapper;
    }

    @GetMapping
    public ResponseEntity<List<AdminUserSummaryResponse>> listUsers(
            @RequestParam(required = false)
            @Size(max = 100, message = "El texto de búsqueda no puede exceder 100 caracteres") String query,
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

    @PostMapping("/customers")
    public ResponseEntity<AdminUserDetailResponse> createCustomer(
            @Valid @RequestBody RegisterRequest request,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        UUID adminId = principal != null ? principal.userId() : null;
        AdminCreateUserCommand command = new AdminCreateUserCommand(
                request.email(),
                request.password(),
                request.firstName(),
                request.lastName(),
                request.phone(),
                adminId
        );
        User user = adminCreateUserService.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseMapper.toDetail(user));
    }

    @PostMapping("/merchants")
    public ResponseEntity<AdminUserDetailResponse> createMerchant(
            @Valid @RequestBody RegisterMerchantRequest request,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        UUID adminId = principal != null ? principal.userId() : null;
        RegisterMerchantCommand command = new RegisterMerchantCommand(
                request.email(),
                request.password(),
                request.firstName(),
                request.lastName(),
                request.phone(),
                request.storeName(),
                request.businessType(),
                request.storeContactPhone(),
                request.address(),
                request.addressReference(),
                request.latitude(),
                request.longitude(),
                request.pickupEnabled(),
                request.deliveryEnabled(),
                request.logoUrl(),
                request.bannerUrl(),
                request.categoryIds()
        );

        AuthResult result = adminCreateMerchantService.execute(command, adminId);
        User user = getAdminUsersService.execute(result.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(responseMapper.toDetail(user));
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

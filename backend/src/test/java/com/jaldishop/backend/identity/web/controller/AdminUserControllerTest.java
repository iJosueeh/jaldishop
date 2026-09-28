package com.jaldishop.backend.identity.web.controller;

import com.jaldishop.backend.identity.application.ChangeUserStatusCommand;
import com.jaldishop.backend.identity.application.ChangeUserStatusService;
import com.jaldishop.backend.identity.application.GetAdminUsersQuery;
import com.jaldishop.backend.identity.application.GetAdminUsersService;
import com.jaldishop.backend.identity.application.UpdateUserRolesService;
import com.jaldishop.backend.identity.domain.Role;
import com.jaldishop.backend.identity.domain.RoleName;
import com.jaldishop.backend.identity.domain.User;
import com.jaldishop.backend.identity.domain.UserStatus;
import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import com.jaldishop.backend.identity.web.dto.AdminUserDetailResponse;
import com.jaldishop.backend.identity.web.dto.AdminUserSummaryResponse;
import com.jaldishop.backend.identity.web.mapper.AdminUserResponseMapper;
import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private GetAdminUsersService getAdminUsersService;

    @Mock
    private ChangeUserStatusService changeUserStatusService;

    @Mock
    private UpdateUserRolesService updateUserRolesService;

    @Mock
    private AdminUserResponseMapper responseMapper;

    private MockMvc mockMvc;
    private UUID adminId;
    private UUID targetUserId;
    private JwtPrincipal adminPrincipal;
    private User testUser;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        targetUserId = UUID.randomUUID();
        adminPrincipal = new JwtPrincipal(adminId, Set.of("ADMIN"));

        testUser = User.reconstitute(
                targetUserId,
                "user@jaldishop.com",
                "hashed",
                "Carlos",
                "Lopez",
                "987654321",
                UserStatus.ACTIVE,
                Set.of(new Role((short) 1, RoleName.CUSTOMER)),
                Instant.now(),
                Instant.now()
        );

        AdminUserController controller = new AdminUserController(
                getAdminUsersService,
                changeUserStatusService,
                updateUserRolesService,
                responseMapper
        );

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return adminPrincipal;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/admin/users - Debe listar usuarios")
    void shouldListUsers() throws Exception {
        AdminUserSummaryResponse summary = new AdminUserSummaryResponse(
                targetUserId,
                "user@jaldishop.com",
                "Carlos",
                "Lopez",
                "Carlos Lopez",
                "987654321",
                UserStatus.ACTIVE,
                Set.of("CUSTOMER"),
                Instant.now(),
                Instant.now(),
                null,
                null
        );

        when(getAdminUsersService.execute(any(GetAdminUsersQuery.class))).thenReturn(List.of(testUser));
        when(responseMapper.toSummary(testUser)).thenReturn(summary);

        mockMvc.perform(get("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("user@jaldishop.com"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/users/{id} - Debe obtener detalle de usuario")
    void shouldGetUserById() throws Exception {
        AdminUserDetailResponse detail = new AdminUserDetailResponse(
                targetUserId,
                "user@jaldishop.com",
                "Carlos",
                "Lopez",
                "Carlos Lopez",
                "987654321",
                UserStatus.ACTIVE,
                Set.of("CUSTOMER"),
                Instant.now(),
                Instant.now(),
                null
        );

        when(getAdminUsersService.execute(targetUserId)).thenReturn(testUser);
        when(responseMapper.toDetail(testUser)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/admin/users/{id}", targetUserId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Carlos Lopez"));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{id}/suspend - Debe suspender usuario")
    void shouldSuspendUser() throws Exception {
        AdminUserDetailResponse suspended = new AdminUserDetailResponse(
                targetUserId,
                "user@jaldishop.com",
                "Carlos",
                "Lopez",
                "Carlos Lopez",
                "987654321",
                UserStatus.SUSPENDED,
                Set.of("CUSTOMER"),
                Instant.now(),
                Instant.now(),
                null
        );

        when(changeUserStatusService.execute(any(ChangeUserStatusCommand.class))).thenReturn(testUser);
        when(responseMapper.toDetail(testUser)).thenReturn(suspended);

        mockMvc.perform(patch("/api/v1/admin/users/{id}/suspend", targetUserId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{id}/activate - Debe reactivar usuario")
    void shouldActivateUser() throws Exception {
        AdminUserDetailResponse activated = new AdminUserDetailResponse(
                targetUserId,
                "user@jaldishop.com",
                "Carlos",
                "Lopez",
                "Carlos Lopez",
                "987654321",
                UserStatus.ACTIVE,
                Set.of("CUSTOMER"),
                Instant.now(),
                Instant.now(),
                null
        );

        when(changeUserStatusService.execute(any(ChangeUserStatusCommand.class))).thenReturn(testUser);
        when(responseMapper.toDetail(testUser)).thenReturn(activated);

        mockMvc.perform(patch("/api/v1/admin/users/{id}/activate", targetUserId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{id}/roles - Debe actualizar roles de usuario")
    void shouldUpdateUserRoles() throws Exception {
        AdminUserDetailResponse updated = new AdminUserDetailResponse(
                targetUserId,
                "user@jaldishop.com",
                "Carlos",
                "Lopez",
                "Carlos Lopez",
                "987654321",
                UserStatus.ACTIVE,
                Set.of("MERCHANT", "CUSTOMER"),
                Instant.now(),
                Instant.now(),
                null
        );

        when(updateUserRolesService.execute(eq(targetUserId), any(), eq(adminId))).thenReturn(testUser);
        when(responseMapper.toDetail(testUser)).thenReturn(updated);

        String requestBody = """
                {
                    "roles": ["MERCHANT", "CUSTOMER"]
                }
                """;

        mockMvc.perform(patch("/api/v1/admin/users/{id}/roles", targetUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").isArray());
    }
}

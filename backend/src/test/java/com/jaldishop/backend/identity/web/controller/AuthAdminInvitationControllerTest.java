package com.jaldishop.backend.identity.web.controller;

import com.jaldishop.backend.identity.application.command.AcceptAdminInvitationCommand;
import com.jaldishop.backend.identity.application.service.AcceptAdminInvitationService;
import com.jaldishop.backend.identity.domain.enums.RoleName;
import com.jaldishop.backend.identity.domain.enums.UserStatus;
import com.jaldishop.backend.identity.domain.model.Role;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.web.dto.AcceptAdminInvitationRequest;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthAdminInvitationControllerTest {

    @Mock
    private AcceptAdminInvitationService acceptAdminInvitationService;

    @InjectMocks
    private AuthAdminInvitationController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/admin-invitations/accept - Debe aceptar invitación y retornar 201")
    void shouldAcceptInvitationSuccessfully() throws Exception {
        User createdAdmin = User.reconstitute(
                UUID.randomUUID(),
                "admin.nuevo@jaldishop.com",
                "hashed",
                "Nuevo",
                "Admin",
                "987654321",
                UserStatus.ACTIVE,
                Set.of(new Role((short) 3, RoleName.ADMIN)),
                Instant.now(),
                Instant.now(),
                false
        );

        when(acceptAdminInvitationService.execute(any(AcceptAdminInvitationCommand.class))).thenReturn(createdAdmin);

        AcceptAdminInvitationRequest request = new AcceptAdminInvitationRequest("valid-token", "NewPassword123!");

        mockMvc.perform(post("/api/v1/auth/admin-invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("admin.nuevo@jaldishop.com"))
                .andExpect(jsonPath("$.firstName").value("Nuevo"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/admin-invitations/accept - Token inválido o expirado retorna 409 Conflict")
    void shouldReturnConflictOnInvalidToken() throws Exception {
        when(acceptAdminInvitationService.execute(any(AcceptAdminInvitationCommand.class)))
                .thenThrow(new ConflictException("INVALID_OR_EXPIRED_INVITATION", "El token ha expirado."));

        AcceptAdminInvitationRequest request = new AcceptAdminInvitationRequest("expired-token", "NewPassword123!");

        mockMvc.perform(post("/api/v1/auth/admin-invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_OR_EXPIRED_INVITATION"));
    }
}

package com.jaldishop.backend.identity.web.controller;

import com.jaldishop.backend.identity.application.service.GetMyProfileService;
import com.jaldishop.backend.identity.application.command.UpdateProfileCommand;
import com.jaldishop.backend.identity.application.service.UpdateProfileService;
import com.jaldishop.backend.identity.domain.model.Role;
import com.jaldishop.backend.identity.domain.enums.RoleName;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.enums.UserStatus;
import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import com.jaldishop.backend.shared.util.MethodValidationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private GetMyProfileService getMyProfileService;

    @Mock
    private UpdateProfileService updateProfileService;

    private MockMvc mockMvc;
    private UUID testUserId;
    private JwtPrincipal currentPrincipal;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        currentPrincipal = new JwtPrincipal(testUserId, Set.of("CUSTOMER"));

        testUser = User.reconstitute(
                testUserId,
                "user@jaldishop.com",
                "hashedPassword",
                "Carlos",
                "Lopez",
                "987654321",
                UserStatus.ACTIVE,
                Set.of(new Role((short) 1, RoleName.CUSTOMER)),
                Instant.now(),
                Instant.now()
        );

        UserController controller = new UserController(getMyProfileService, updateProfileService);

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                        && parameter.getParameterType().isAssignableFrom(JwtPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                if (currentPrincipal == null) {
                    throw new BadCredentialsException("Autenticación requerida");
                }
                return currentPrincipal;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(MethodValidationTestSupport.withParameterValidation(controller))
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/users/me - 200 OK con el perfil del usuario autenticado")
    void getMyProfileSuccess() throws Exception {
        when(getMyProfileService.execute(testUserId)).thenReturn(testUser);

        mockMvc.perform(get("/api/v1/users/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUserId.toString()))
                .andExpect(jsonPath("$.email").value("user@jaldishop.com"))
                .andExpect(jsonPath("$.firstName").value("Carlos"))
                .andExpect(jsonPath("$.lastName").value("Lopez"))
                .andExpect(jsonPath("$.phone").value("987654321"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.roles[0]").value("CUSTOMER"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/users/me - 401 Unauthorized cuando no hay credenciales")
    void getMyProfileUnauthorized() throws Exception {
        currentPrincipal = null;

        mockMvc.perform(get("/api/v1/users/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Autenticación requerida"));

        verifyNoInteractions(getMyProfileService);
    }

    @Test
    @DisplayName("GET /api/v1/users/me - 404 RESOURCE_NOT_FOUND si el usuario no existe")
    void getMyProfileNotFound() throws Exception {
        when(getMyProfileService.execute(testUserId))
                .thenThrow(new ResourceNotFoundException("Usuario no encontrado."));

        mockMvc.perform(get("/api/v1/users/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Usuario no encontrado."));
    }

    @Test
    @DisplayName("PUT /api/v1/users/me - 200 OK actualiza el perfil correctamente")
    void updateProfileSuccess() throws Exception {
        User updatedUser = User.reconstitute(
                testUserId,
                "user@jaldishop.com",
                "hashedPassword",
                "Juan",
                "Pérez",
                "912345678",
                UserStatus.ACTIVE,
                Set.of(new Role((short) 1, RoleName.CUSTOMER)),
                Instant.now(),
                Instant.now()
        );

        when(updateProfileService.execute(any(UpdateProfileCommand.class))).thenReturn(updatedUser);

        String requestJson = """
                {
                    "firstName": "Juan",
                    "lastName": "Pérez",
                    "phone": "912345678"
                }
                """;

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUserId.toString()))
                .andExpect(jsonPath("$.firstName").value("Juan"))
                .andExpect(jsonPath("$.lastName").value("Pérez"))
                .andExpect(jsonPath("$.phone").value("912345678"));
    }

    @Test
    @DisplayName("PUT /api/v1/users/me - 400 VALIDATION_ERROR cuando firstName es blanco")
    void updateProfileValidationMissingFirstName() throws Exception {
        String requestJson = """
                {
                    "firstName": "   ",
                    "lastName": "Pérez",
                    "phone": "912345678"
                }
                """;

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.firstName").value("El nombre es obligatorio"));

        verifyNoInteractions(updateProfileService);
    }

    @Test
    @DisplayName("PUT /api/v1/users/me - 400 VALIDATION_ERROR cuando lastName es blanco")
    void updateProfileValidationMissingLastName() throws Exception {
        String requestJson = """
                {
                    "firstName": "Juan",
                    "lastName": "",
                    "phone": "912345678"
                }
                """;

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.lastName").value("El apellido es obligatorio"));

        verifyNoInteractions(updateProfileService);
    }

    @Test
    @DisplayName("PUT /api/v1/users/me - 400 VALIDATION_ERROR cuando phone tiene formato inválido")
    void updateProfileValidationInvalidPhone() throws Exception {
        String requestJson = """
                {
                    "firstName": "Juan",
                    "lastName": "Pérez",
                    "phone": "12345"
                }
                """;

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.phone").isNotEmpty());

        verifyNoInteractions(updateProfileService);
    }

    @Test
    @DisplayName("PUT /api/v1/users/me - 400 MALFORMED_JSON con cuerpo malformado")
    void updateProfileMalformedJson() throws Exception {
        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));

        verifyNoInteractions(updateProfileService);
    }

    @Test
    @DisplayName("PUT /api/v1/users/me - 401 Unauthorized cuando no hay credenciales")
    void updateProfileUnauthorized() throws Exception {
        currentPrincipal = null;

        String requestJson = """
                {
                    "firstName": "Juan",
                    "lastName": "Pérez",
                    "phone": "912345678"
                }
                """;

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Autenticación requerida"));

        verifyNoInteractions(updateProfileService);
    }
}

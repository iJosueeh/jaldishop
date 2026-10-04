package com.jaldishop.backend.identity.web.controller;

import com.jaldishop.backend.identity.application.AuthResult;
import com.jaldishop.backend.identity.application.AuthenticateUserService;
import com.jaldishop.backend.identity.application.RegisterCustomerService;
import com.jaldishop.backend.identity.application.RegisterMerchantCommand;
import com.jaldishop.backend.identity.application.RegisterMerchantService;
import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticateUserService authenticateUserService;

    @Mock
    private RegisterCustomerService registerCustomerService;

    @Mock
    private RegisterMerchantService registerMerchantService;

    @Mock
    private com.jaldishop.backend.identity.application.RefreshTokenService refreshTokenService;

    private MockMvc mockMvc;
    private UUID testUserId;
    private com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal currentPrincipal;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        currentPrincipal = new com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal(testUserId, Set.of("CUSTOMER"));

        AuthController controller = new AuthController(
                authenticateUserService,
                registerCustomerService,
                registerMerchantService,
                refreshTokenService
        );

        org.springframework.web.method.support.HandlerMethodArgumentResolver authPrincipalResolver =
                new org.springframework.web.method.support.HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(org.springframework.core.MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(org.springframework.security.core.annotation.AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(org.springframework.core.MethodParameter parameter,
                                                  org.springframework.web.method.support.ModelAndViewContainer mavContainer,
                                                  org.springframework.web.context.request.NativeWebRequest webRequest,
                                                  org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
                        return currentPrincipal;
                    }
                };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authPrincipalResolver)
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register/merchant - 201 Created cuando los datos son válidos")
    void registerMerchantSuccessfully() throws Exception {
        UUID userId = UUID.randomUUID();
        AuthResult authResult = new AuthResult(
                "mocked-jwt-token",
                userId,
                "comerciante@test.com",
                "Carlos Mendoza",
                Set.of("MERCHANT", "CUSTOMER")
        );

        when(registerMerchantService.execute(any(RegisterMerchantCommand.class))).thenReturn(authResult);

        String requestJson = """
                {
                    "email": "comerciante@test.com",
                    "password": "password123",
                    "firstName": "Carlos",
                    "lastName": "Mendoza",
                    "phone": "987654321",
                    "storeName": "Mi Pastelería",
                    "businessType": "Repostería",
                    "storeContactPhone": "987654321",
                    "address": "Av. Los Fresnos 123",
                    "pickupEnabled": true,
                    "deliveryEnabled": true
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register/merchant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("mocked-jwt-token"))
                .andExpect(jsonPath("$.email").value("comerciante@test.com"))
                .andExpect(jsonPath("$.fullName").value("Carlos Mendoza"))
                .andExpect(jsonPath("$.roles").isArray());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register/merchant - 400 Bad Request cuando faltan campos obligatorios")
    void registerMerchantValidationErrors() throws Exception {
        String invalidJson = """
                {
                    "email": "invalid-email",
                    "password": "123",
                    "firstName": "",
                    "lastName": "",
                    "phone": "123",
                    "storeName": "",
                    "pickupEnabled": true,
                    "deliveryEnabled": true
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register/merchant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.lastName").exists())
                .andExpect(jsonPath("$.errors.storeName").exists());
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - 200 OK con nuevo token y roles")
    void refreshTokenSuccessfully() throws Exception {
        AuthResult authResult = new AuthResult(
                "refreshed-jwt-token",
                testUserId,
                "user@test.com",
                "Carlos Mendoza",
                Set.of("MERCHANT", "CUSTOMER")
        );

        when(refreshTokenService.execute(testUserId)).thenReturn(authResult);

        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("refreshed-jwt-token"))
                .andExpect(jsonPath("$.roles").isArray());
    }
}

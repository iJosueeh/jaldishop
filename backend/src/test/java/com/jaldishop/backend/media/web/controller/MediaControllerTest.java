package com.jaldishop.backend.media.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import com.jaldishop.backend.media.application.GenerateUploadSignatureCommand;
import com.jaldishop.backend.media.application.GenerateUploadSignatureService;
import com.jaldishop.backend.media.domain.MediaTargetType;
import com.jaldishop.backend.media.domain.UploadSignature;
import com.jaldishop.backend.media.web.dto.GenerateSignatureRequest;
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

import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MediaControllerTest {

    @Mock
    private GenerateUploadSignatureService signatureService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private JwtPrincipal currentPrincipal;

    @BeforeEach
    void setUp() {
        currentPrincipal = new JwtPrincipal(UUID.randomUUID(), Set.of("MERCHANT"));
        MediaController controller = new MediaController(signatureService);

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                        && parameter.getParameterType().isAssignableFrom(JwtPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return currentPrincipal;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authPrincipalResolver)
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/media/upload-signature - Generar firma para Merchant (200 OK)")
    void generateUploadSignatureSuccess() throws Exception {
        UploadSignature signature = new UploadSignature(
                "demo-cloud",
                "api-key-123",
                1700000000L,
                "jaldishop/tenants/store-id/branding",
                "sig-hash",
                "https://api.cloudinary.com/v1_1/demo-cloud/image/upload"
        );
        when(signatureService.execute(any(GenerateUploadSignatureCommand.class))).thenReturn(signature);

        GenerateSignatureRequest request = new GenerateSignatureRequest(MediaTargetType.STORE_LOGO, null);

        mockMvc.perform(post("/api/v1/media/upload-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cloudName").value("demo-cloud"))
                .andExpect(jsonPath("$.apiKey").value("api-key-123"))
                .andExpect(jsonPath("$.folder").value("jaldishop/tenants/store-id/branding"))
                .andExpect(jsonPath("$.signature").value("sig-hash"))
                .andExpect(jsonPath("$.uploadUrl").value("https://api.cloudinary.com/v1_1/demo-cloud/image/upload"));
    }

    @Test
    @DisplayName("POST /api/v1/media/upload-signature - Rechazar usuario sin rol autorizado (403 FORBIDDEN)")
    void rejectUnauthorizedUser() throws Exception {
        currentPrincipal = new JwtPrincipal(UUID.randomUUID(), Set.of("CUSTOMER"));

        GenerateSignatureRequest request = new GenerateSignatureRequest(MediaTargetType.STORE_LOGO, null);

        mockMvc.perform(post("/api/v1/media/upload-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}

package com.jaldishop.backend.media.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import com.jaldishop.backend.media.application.command.GenerateUploadSignatureCommand;
import com.jaldishop.backend.media.application.service.GenerateUploadSignatureService;
import com.jaldishop.backend.media.domain.enums.MediaTargetType;
import com.jaldishop.backend.media.domain.model.UploadSignature;
import com.jaldishop.backend.media.domain.repository.MediaStorageService;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
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
    private HandlerMethodArgumentResolver authPrincipalResolver;

    @BeforeEach
    void setUp() {
        currentPrincipal = new JwtPrincipal(UUID.randomUUID(), Set.of("MERCHANT"));
        MediaController controller = new MediaController(signatureService);

        authPrincipalResolver = new HandlerMethodArgumentResolver() {
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
    @DisplayName("POST /api/v1/media/upload-signature - Rechazar usuario sin rol autorizado para producto (403 FORBIDDEN)")
    void rejectUnauthorizedUserForProduct() throws Exception {
        currentPrincipal = new JwtPrincipal(UUID.randomUUID(), Set.of("CUSTOMER"));

        GenerateSignatureRequest request = new GenerateSignatureRequest(MediaTargetType.PRODUCT_IMAGE, null);

        mockMvc.perform(post("/api/v1/media/upload-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(signatureService);
    }

    @Test
    void foreignStoreAndAnonymousExplicitStoreReturn404WithoutSigning() throws Exception {
        StoreRepository stores = mock(StoreRepository.class);
        MediaStorageService storage = mock(MediaStorageService.class);
        Store ownStore = mock(Store.class);
        UUID foreignStoreId = UUID.randomUUID();
        when(ownStore.getId()).thenReturn(UUID.randomUUID());
        when(stores.findByMerchantUserId(currentPrincipal.userId())).thenReturn(Optional.of(ownStore));
        MockMvc realController = MockMvcBuilders.standaloneSetup(
                        new MediaController(new GenerateUploadSignatureService(storage, stores)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(authPrincipalResolver)
                .build();

        realController.perform(post("/api/v1/media/upload-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GenerateSignatureRequest(MediaTargetType.PRODUCT_IMAGE, foreignStoreId))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        currentPrincipal = null;
        realController.perform(post("/api/v1/media/upload-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GenerateSignatureRequest(MediaTargetType.STORE_LOGO, foreignStoreId))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        verifyNoInteractions(storage);
    }

    @Test
    @DisplayName("POST /api/v1/media/upload-signature - Permitir logo de tienda para Customer en onboarding (200 OK)")
    void allowStoreLogoForCustomerDuringOnboarding() throws Exception {
        currentPrincipal = new JwtPrincipal(UUID.randomUUID(), Set.of("CUSTOMER"));

        UploadSignature signature = new UploadSignature(
                "demo-cloud",
                "api-key-123",
                1700000000L,
                "jaldishop/tenants/common/branding",
                "sig-hash",
                "https://api.cloudinary.com/v1_1/demo-cloud/image/upload"
        );
        when(signatureService.execute(any(GenerateUploadSignatureCommand.class))).thenReturn(signature);

        GenerateSignatureRequest request = new GenerateSignatureRequest(MediaTargetType.STORE_LOGO, null);

        mockMvc.perform(post("/api/v1/media/upload-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cloudName").value("demo-cloud"));
    }
}

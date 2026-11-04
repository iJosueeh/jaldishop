package com.jaldishop.backend.shared.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class RestAccessDeniedHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestAccessDeniedHandler handler = new RestAccessDeniedHandler(objectMapper);

    @Test
    @DisplayName("handle() debe retornar HTTP 403 con ApiError (FORBIDDEN)")
    void handleShouldReturn403WithApiError() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AccessDeniedException exception = new AccessDeniedException("Forbidden access");

        handler.handle(request, response, exception);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/json");

        String content = response.getContentAsString();
        ApiError apiError = objectMapper.readValue(content, ApiError.class);

        assertThat(apiError.code()).isEqualTo("FORBIDDEN");
        assertThat(apiError.message()).isEqualTo("No tienes permisos para realizar esta operación");
        assertThat(apiError.errors()).isNull();
    }
}

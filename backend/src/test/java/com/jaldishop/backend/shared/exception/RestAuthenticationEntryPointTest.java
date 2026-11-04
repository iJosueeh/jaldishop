package com.jaldishop.backend.shared.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class RestAuthenticationEntryPointTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(objectMapper);

    @Test
    @DisplayName("commence() debe retornar HTTP 401 con ApiError (UNAUTHORIZED)")
    void commenceShouldReturn401WithApiError() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        BadCredentialsException exception = new BadCredentialsException("Token missing");

        entryPoint.commence(request, response, exception);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json");

        String content = response.getContentAsString();
        ApiError apiError = objectMapper.readValue(content, ApiError.class);

        assertThat(apiError.code()).isEqualTo("UNAUTHORIZED");
        assertThat(apiError.message()).isEqualTo("Autenticación requerida");
        assertThat(apiError.errors()).isNull();
    }
}

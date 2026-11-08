package com.jaldishop.backend.favorite.web.controller;

import com.jaldishop.backend.favorite.application.service.AddFavoriteProductService;
import com.jaldishop.backend.favorite.application.service.CheckFavoriteProductService;
import com.jaldishop.backend.favorite.application.service.GetCustomerFavoriteProductsService;
import com.jaldishop.backend.favorite.application.service.RemoveFavoriteProductService;
import com.jaldishop.backend.favorite.web.dto.FavoriteStatusResponse;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.identity.infrastructure.security.JwtAuthenticationFilter;
import com.jaldishop.backend.identity.infrastructure.security.JwtService;
import com.jaldishop.backend.identity.infrastructure.security.SecurityConfig;
import com.jaldishop.backend.identity.infrastructure.security.WebCorsConfiguration;
import com.jaldishop.backend.shared.exception.GlobalExceptionHandler;
import com.jaldishop.backend.shared.exception.RestAccessDeniedHandler;
import com.jaldishop.backend.shared.exception.RestAuthenticationEntryPoint;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(properties = "spring.config.import=")
@ContextConfiguration(classes = {
        CustomerFavoriteController.class, GlobalExceptionHandler.class,
        SecurityConfig.class, WebCorsConfiguration.class, JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class
})
class CustomerFavoriteHttpRoutingTest {
    private static final String PRODUCTS = "/api/v1/customer/favorites/products";
    private static final UUID PRODUCT_ID = UUID.fromString("b1ff6408-2c6c-4ff3-bb24-8fd5c03754f7");
    private static final UUID CUSTOMER_ID = UUID.fromString("7b1442ae-7dbe-4ac5-95d5-b206105112d9");

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AddFavoriteProductService addFavoriteProductService;
    @MockitoBean private RemoveFavoriteProductService removeFavoriteProductService;
    @MockitoBean private CheckFavoriteProductService checkFavoriteProductService;
    @MockitoBean private GetCustomerFavoriteProductsService getCustomerFavoriteProductsService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserRepository userRepository;

    @Test
    void validUuidReturnsFavoriteStatus() throws Exception {
        when(checkFavoriteProductService.execute(CUSTOMER_ID, PRODUCT_ID))
                .thenReturn(new FavoriteStatusResponse(PRODUCT_ID, false));

        asCustomer(get(PRODUCTS + "/" + PRODUCT_ID + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(PRODUCT_ID.toString()))
                .andExpect(jsonPath("$.isFavorite").value(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-uuid", "%7BproductId%7D", "%7B%7BproductId%7D%7D", ":productId"})
    void malformedUuidAndUnresolvedVariablesReturn400(String segment) throws Exception {
        expectError(asCustomer(get(URI.create(PRODUCTS + "/" + segment + "/status"))),
                400, "INVALID_PARAMETER", "El parámetro 'productId' tiene un formato no válido");
        verifyNoFavoriteServiceCalls();
    }

    @Test
    void omittedSegmentMatchesPostAndDeletePathBeforeUuidConversion() throws Exception {
        var result = expectError(asCustomer(get(PRODUCTS + "/status")),
                405, "METHOD_NOT_ALLOWED", "El método HTTP 'GET' no está soportado para esta ruta")
                .andExpect(header().string(HttpHeaders.ALLOW, containsString("POST")))
                .andExpect(header().string(HttpHeaders.ALLOW, containsString("DELETE")))
                .andReturn();
        assertThat(result.getResolvedException())
                .isInstanceOf(org.springframework.web.HttpRequestMethodNotSupportedException.class);
        verifyNoFavoriteServiceCalls();
    }

    @Test
    void statusIsConvertedAsProductIdWhenUsingAnAllowedMethod() throws Exception {
        expectError(asCustomer(post(PRODUCTS + "/status")),
                400, "INVALID_PARAMETER", "El parámetro 'productId' tiene un formato no válido");
        verifyNoFavoriteServiceCalls();
    }

    @Test
    void emptySegmentIsRejectedBySecurityFirewallWithoutRedirect() throws Exception {
        // URI overload preserves the double slash instead of normalizing the URI template in MockMvc.
        expectError(asCustomer(get(URI.create(PRODUCTS + "//status"))),
                400, "INVALID_PARAMETER", "Parámetros de consulta o ruta no válidos");
        verifyNoFavoriteServiceCalls();
    }

    @Test
    void emptySegmentWithoutSecurityDoesNotMatchAnyController() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
                        new CustomerFavoriteController(addFavoriteProductService, removeFavoriteProductService,
                                checkFavoriteProductService, getCustomerFavoriteProductsService))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        expectError(mvc.perform(get(URI.create(PRODUCTS + "//status"))),
                404, "RESOURCE_NOT_FOUND", "El recurso solicitado '" + PRODUCTS + "//status' no fue encontrado");
        verifyNoFavoriteServiceCalls();
    }

    @Test
    void nonexistentRouteReturnsNormalized404() throws Exception {
        expectError(asCustomer(get(PRODUCTS + "/" + PRODUCT_ID + "/status/unknown")),
                404, "RESOURCE_NOT_FOUND",
                "El recurso solicitado 'api/v1/customer/favorites/products/" + PRODUCT_ID
                        + "/status/unknown' no fue encontrado");
        verifyNoFavoriteServiceCalls();
    }

    @Test
    void unsupportedMethodOnStatusReturns405() throws Exception {
        expectError(asCustomer(post(PRODUCTS + "/" + PRODUCT_ID + "/status")),
                405, "METHOD_NOT_ALLOWED", "El método HTTP 'POST' no está soportado para esta ruta")
                .andExpect(header().string(HttpHeaders.ALLOW, containsString("GET")));
        verifyNoFavoriteServiceCalls();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/b1ff6408-2c6c-4ff3-bb24-8fd5c03754f7/status", "/not-a-uuid/status",
            "/status", "/unknown/route/extra"})
    void missingAuthenticationRemains401(String path) throws Exception {
        expectError(mockMvc.perform(get(PRODUCTS + path)),
                401, "UNAUTHORIZED", "Autenticación requerida");
        verifyNoFavoriteServiceCalls();
    }

    @Test
    void merchantRemainsForbidden() throws Exception {
        expectError(mockMvc.perform(get(PRODUCTS + "/" + PRODUCT_ID + "/status")
                        .with(authentication(auth("MERCHANT")))),
                403, "FORBIDDEN", "No tienes permisos para realizar esta operación");
        verifyNoFavoriteServiceCalls();
    }

    private ResultActions asCustomer(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.with(authentication(auth("CUSTOMER"))));
    }

    private UsernamePasswordAuthenticationToken auth(String role) {
        return new UsernamePasswordAuthenticationToken(new JwtPrincipal(CUSTOMER_ID, Set.of(role)),
                null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    private ResultActions expectError(ResultActions result, int status, String code, String message) throws Exception {
        return result.andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.errors").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(header().doesNotExist(HttpHeaders.LOCATION));
    }

    private void verifyNoFavoriteServiceCalls() {
        verifyNoInteractions(addFavoriteProductService, removeFavoriteProductService,
                checkFavoriteProductService, getCustomerFavoriteProductsService);
    }
}

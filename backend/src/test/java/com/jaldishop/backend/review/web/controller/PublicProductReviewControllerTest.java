package com.jaldishop.backend.review.web.controller;

import com.jaldishop.backend.review.application.GetProductPublicReviewsService;
import com.jaldishop.backend.review.web.dto.ProductReviewsSummaryResponse;
import com.jaldishop.backend.review.web.dto.PublicReviewResponse;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PublicProductReviewControllerTest {

    @Mock
    private GetProductPublicReviewsService getProductPublicReviewsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PublicProductReviewController controller = new PublicProductReviewController(getProductPublicReviewsService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/products/{productId}/reviews retorna el resumen y lista de reseñas públicas")
    void shouldReturnPublicReviews() throws Exception {
        UUID productId = UUID.randomUUID();
        PublicReviewResponse review = new PublicReviewResponse(
                UUID.randomUUID(), "María C.", 5, "Hermoso acabado.", Instant.now()
        );
        ProductReviewsSummaryResponse summary = new ProductReviewsSummaryResponse(
                productId, 5.0, 1, List.of(review)
        );

        when(getProductPublicReviewsService.execute(productId)).thenReturn(summary);

        mockMvc.perform(get("/api/v1/products/{productId}/reviews", productId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId.toString()))
                .andExpect(jsonPath("$.averageRating").value(5.0))
                .andExpect(jsonPath("$.totalReviews").value(1))
                .andExpect(jsonPath("$.reviews[0].customerName").value("María C."));
    }
}

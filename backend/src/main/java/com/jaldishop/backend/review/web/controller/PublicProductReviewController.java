package com.jaldishop.backend.review.web.controller;

import com.jaldishop.backend.review.application.GetProductPublicReviewsService;
import com.jaldishop.backend.review.web.dto.ProductReviewsSummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products/{productId}/reviews")
public class PublicProductReviewController {

    private final GetProductPublicReviewsService getProductPublicReviewsService;

    public PublicProductReviewController(GetProductPublicReviewsService getProductPublicReviewsService) {
        this.getProductPublicReviewsService = getProductPublicReviewsService;
    }

    @GetMapping
    public ResponseEntity<ProductReviewsSummaryResponse> getProductReviews(@PathVariable("productId") UUID productId) {
        ProductReviewsSummaryResponse response = getProductPublicReviewsService.execute(productId);
        return ResponseEntity.ok(response);
    }
}

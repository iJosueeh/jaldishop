package com.jaldishop.backend.review.web.controller;

import com.jaldishop.backend.review.application.CheckReviewEligibilityService;
import com.jaldishop.backend.review.application.CreateProductReviewService;
import com.jaldishop.backend.review.application.GetCustomerReviewsService;
import com.jaldishop.backend.review.application.UpdateProductReviewService;
import com.jaldishop.backend.review.web.dto.CreateReviewRequest;
import com.jaldishop.backend.review.web.dto.CustomerReviewResponse;
import com.jaldishop.backend.review.web.dto.ReviewEligibilityResponse;
import com.jaldishop.backend.review.web.dto.UpdateReviewRequest;
import com.jaldishop.backend.shared.security.JwtPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerReviewController {

    private final CheckReviewEligibilityService checkReviewEligibilityService;
    private final CreateProductReviewService createProductReviewService;
    private final UpdateProductReviewService updateProductReviewService;
    private final GetCustomerReviewsService getCustomerReviewsService;

    public CustomerReviewController(CheckReviewEligibilityService checkReviewEligibilityService,
                                    CreateProductReviewService createProductReviewService,
                                    UpdateProductReviewService updateProductReviewService,
                                    GetCustomerReviewsService getCustomerReviewsService) {
        this.checkReviewEligibilityService = checkReviewEligibilityService;
        this.createProductReviewService = createProductReviewService;
        this.updateProductReviewService = updateProductReviewService;
        this.getCustomerReviewsService = getCustomerReviewsService;
    }

    @GetMapping("/products/{productId}/reviews/eligibility")
    public ResponseEntity<ReviewEligibilityResponse> checkEligibility(
            @PathVariable("productId") UUID productId,
            @AuthenticationPrincipal JwtPrincipal principal) {
        ReviewEligibilityResponse response = checkReviewEligibilityService.execute(principal.userId(), productId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/products/{productId}/reviews")
    public ResponseEntity<CustomerReviewResponse> createReview(
            @PathVariable("productId") UUID productId,
            @Valid @RequestBody CreateReviewRequest request,
            @AuthenticationPrincipal JwtPrincipal principal) {
        CustomerReviewResponse response = createProductReviewService.execute(principal.userId(), productId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/products/{productId}/reviews")
    public ResponseEntity<CustomerReviewResponse> updateReview(
            @PathVariable("productId") UUID productId,
            @Valid @RequestBody UpdateReviewRequest request,
            @AuthenticationPrincipal JwtPrincipal principal) {
        CustomerReviewResponse response = updateProductReviewService.execute(principal.userId(), productId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reviews")
    public ResponseEntity<List<CustomerReviewResponse>> getCustomerReviews(
            @AuthenticationPrincipal JwtPrincipal principal) {
        List<CustomerReviewResponse> reviews = getCustomerReviewsService.execute(principal.userId());
        return ResponseEntity.ok(reviews);
    }
}

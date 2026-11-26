package com.jaldishop.backend.promo.web.controller;

import com.jaldishop.backend.promo.application.dto.ActiveBannerResponse;
import com.jaldishop.backend.promo.application.dto.ValidatePromoResponse;
import com.jaldishop.backend.promo.application.service.GetActiveBannerPromoService;
import com.jaldishop.backend.promo.application.service.ValidatePromoCodeService;
import com.jaldishop.backend.promo.domain.model.PromoValidationResult;
import com.jaldishop.backend.promo.web.dto.ValidatePromoRequest;
import com.jaldishop.backend.shared.security.ratelimit.ClientIpResolver;
import com.jaldishop.backend.shared.security.ratelimit.RateLimits;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/public")
public class PublicPromoCodeController {

    private final GetActiveBannerPromoService bannerService;
    private final ValidatePromoCodeService validateService;
    private final RateLimits rateLimits;
    private final ClientIpResolver clientIpResolver;

    public PublicPromoCodeController(
            GetActiveBannerPromoService bannerService,
            ValidatePromoCodeService validateService,
            RateLimits rateLimits,
            ClientIpResolver clientIpResolver
    ) {
        this.bannerService = bannerService;
        this.validateService = validateService;
        this.rateLimits = rateLimits;
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping("/marketplace/active-banner")
    public ResponseEntity<ActiveBannerResponse> getActiveBanner() {
        return ResponseEntity.ok(bannerService.getActiveBanner());
    }

    @PostMapping("/checkout/validate-promo")
    public ResponseEntity<ValidatePromoResponse> validatePromo(
            @Valid @RequestBody ValidatePromoRequest request,
            HttpServletRequest httpRequest
    ) {
        String ip = clientIpResolver.resolveOrRemoteAddr(httpRequest);
        rateLimits.check("promo-validate", ip, 25, Duration.ofMinutes(1));

        PromoValidationResult result = validateService.validateForPreview(
                request.storeId(),
                request.code(),
                request.subtotal()
        );
        return ResponseEntity.ok(ValidatePromoResponse.fromResult(result));
    }
}

package com.jaldishop.backend.promo.application.service;

import com.jaldishop.backend.promo.domain.model.PromoCode;
import com.jaldishop.backend.promo.domain.model.PromoValidationResult;
import com.jaldishop.backend.promo.domain.repository.PromoCodeRepository;
import com.jaldishop.backend.promo.domain.service.PromoDiscountCalculator;
import com.jaldishop.backend.shared.exception.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ValidatePromoCodeService {

    private final PromoCodeRepository repository;
    private final PromoDiscountCalculator calculator;

    public ValidatePromoCodeService(PromoCodeRepository repository, Clock clock) {
        this.repository = repository;
        this.calculator = new PromoDiscountCalculator(clock);
    }

    public PromoValidationResult validateForPreview(UUID storeId, String rawCode, BigDecimal productsSubtotal) {
        String normalizedCode;
        try {
            normalizedCode = PromoCode.normalizeCode(rawCode);
        } catch (BusinessRuleException ex) {
            return PromoValidationResult.invalid(
                    rawCode != null ? rawCode.trim() : null,
                    ex.getCode(),
                    ex.getMessage(),
                    productsSubtotal
            );
        }

        PromoCode promo = repository.findByCodeIgnoreCase(normalizedCode).orElse(null);
        return calculator.validateAndCalculate(promo, storeId, productsSubtotal);
    }

    public PromoValidationResult validateOrThrow(UUID storeId, String rawCode, BigDecimal productsSubtotal) {
        String normalizedCode = PromoCode.normalizeCode(rawCode);
        PromoCode promo = repository.findByCodeIgnoreCase(normalizedCode).orElse(null);
        PromoValidationResult result = calculator.validateAndCalculate(promo, storeId, productsSubtotal);
        if (!result.valid()) {
            throw new BusinessRuleException(result.errorCode(), result.errorMessage());
        }
        return result;
    }
}

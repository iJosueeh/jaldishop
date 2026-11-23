package com.jaldishop.backend.store.web.mapper;

import com.jaldishop.backend.billing.application.service.MerchantBillingQueryService;
import com.jaldishop.backend.billing.web.dto.MerchantPayoutDestinationResponse;
import com.jaldishop.backend.identity.domain.model.User;
import com.jaldishop.backend.identity.domain.repository.UserRepository;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.model.StoreTaxProfile;
import com.jaldishop.backend.store.web.dto.AdminStoreDetailResponse;
import com.jaldishop.backend.store.web.dto.AdminStoreOwnerResponse;
import com.jaldishop.backend.store.web.dto.AdminStoreSummaryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class AdminStoreResponseMapper {

    private final UserRepository userRepository;
    private final MerchantBillingQueryService billingQueryService;

    public AdminStoreResponseMapper(UserRepository userRepository) {
        this(userRepository, null);
    }

    @Autowired
    public AdminStoreResponseMapper(UserRepository userRepository, @Autowired(required = false) MerchantBillingQueryService billingQueryService) {
        this.userRepository = userRepository;
        this.billingQueryService = billingQueryService;
    }

    public AdminStoreSummaryResponse toSummary(Store store) {
        AdminStoreOwnerResponse merchant = fetchMerchant(store.getMerchantUserId());

        return new AdminStoreSummaryResponse(
                store.getId(),
                store.getMerchantUserId(),
                store.getName(),
                store.getSlug(),
                store.getContactPhone(),
                store.getStatus(),
                store.isDeliveryEnabled(),
                store.isPickupEnabled(),
                store.getCreatedAt(),
                store.getUpdatedAt(),
                merchant
        );
    }

    public AdminStoreDetailResponse toDetail(Store store) {
        AdminStoreOwnerResponse merchant = fetchMerchant(store.getMerchantUserId());
        StoreTaxProfile taxProfile = store.getTaxProfile();

        String legalBusinessName = taxProfile != null ? taxProfile.legalBusinessName() : null;
        String taxIdType = taxProfile != null && taxProfile.taxIdType() != null ? taxProfile.taxIdType().name() : null;
        String taxIdNumber = taxProfile != null ? taxProfile.taxIdNumber() : null;
        String taxRegime = taxProfile != null && taxProfile.taxRegime() != null ? taxProfile.taxRegime().name() : null;
        Boolean canIssueInvoices = taxProfile != null ? taxProfile.canIssueInvoices() : false;

        MerchantPayoutDestinationResponse payoutDestination = (billingQueryService != null && store.getId() != null)
                ? billingQueryService.getPrimaryDestination(store.getId())
                : null;

        return new AdminStoreDetailResponse(
                store.getId(),
                store.getMerchantUserId(),
                store.getName(),
                store.getSlug(),
                store.getDescription(),
                store.getContactPhone(),
                store.getAddress(),
                store.getAddressReference(),
                store.getLatitude(),
                store.getLongitude(),
                store.isPickupEnabled(),
                store.isDeliveryEnabled(),
                store.getDeliveryFeeAmount(),
                store.getDeliveryFeeCurrency(),
                store.getTaxRate(),
                store.getLogoUrl(),
                store.getBannerUrl(),
                store.getInstagramUrl(),
                store.getFacebookUrl(),
                store.getWhatsappNumber(),
                store.getStatus(),
                store.getCreatedAt(),
                store.getUpdatedAt(),
                merchant,
                legalBusinessName,
                taxIdType,
                taxIdNumber,
                taxRegime,
                canIssueInvoices,
                payoutDestination
        );
    }

    private AdminStoreOwnerResponse fetchMerchant(UUID merchantUserId) {
        if (merchantUserId == null) return null;
        Optional<User> userOpt = userRepository.findById(merchantUserId);
        return userOpt.map(u -> new AdminStoreOwnerResponse(
                u.getId(),
                u.getEmail(),
                u.getFullName(),
                u.getPhone()
        )).orElse(null);
    }
}

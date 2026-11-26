package com.jaldishop.backend.store.application.service;

import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import com.jaldishop.backend.store.domain.model.Store;
import com.jaldishop.backend.store.domain.model.StoreTaxProfile;
import com.jaldishop.backend.store.domain.repository.StoreRepository;
import com.jaldishop.backend.store.web.dto.UpdateStoreTaxProfileRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class UpdateStoreTaxProfileService {

    private final StoreRepository storeRepository;

    public UpdateStoreTaxProfileService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    public Store execute(UUID merchantUserId, UpdateStoreTaxProfileRequest request) {
        Store store = storeRepository.findByMerchantUserId(merchantUserId)
                .orElseThrow(() -> new ResourceNotFoundException("STORE_NOT_FOUND", "No se encontró una tienda para este comerciante."));

        StoreTaxProfile profile = StoreTaxProfile.createValidated(
                request.legalBusinessName(),
                request.taxIdType(),
                request.taxIdNumber(),
                request.taxRegime(),
                request.canIssueInvoices()
        );

        store.updateTaxProfile(profile, request.taxRate());
        return storeRepository.save(store);
    }
}

package com.jaldishop.backend.store.application;

import com.jaldishop.backend.identity.infrastructure.security.JwtPrincipal;
import com.jaldishop.backend.store.domain.Store;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class StoreContextService {

    private final GetMyStoreService getMyStoreService;

    public StoreContextService(GetMyStoreService getMyStoreService) {
        this.getMyStoreService = getMyStoreService;
    }

    public UUID requireStoreId(JwtPrincipal principal) {
        requireMerchantRole(principal);
        Store store = getMyStoreService.execute(principal.userId());
        return store.getId();
    }

    public Store requireStore(JwtPrincipal principal) {
        requireMerchantRole(principal);
        return getMyStoreService.execute(principal.userId());
    }

    public void requireMerchantRole(JwtPrincipal principal) {
        if (principal == null || !principal.roles().contains("MERCHANT")) {
            throw new AccessDeniedException("Solo los usuarios con el rol MERCHANT pueden realizar esta operación.");
        }
    }

}

package com.jaldishop.backend.store.web.dto;

import com.jaldishop.backend.store.application.CloseStoreResult;
import com.jaldishop.backend.store.domain.CloseStoreAction;
import com.jaldishop.backend.store.domain.StoreStatus;

import java.util.UUID;

public record CloseStoreResponse(
        UUID storeId,
        CloseStoreAction action,
        StoreStatus status,
        String message
) {
    public static CloseStoreResponse fromResult(CloseStoreResult result) {
        String message = result.message() != null ? result.message() : switch (result.action()) {
            case DELETED -> "La tienda y sus configuraciones fueron eliminadas permanentemente al no registrar historial operativo.";
            case DEACTIVATED -> "La tienda fue desactivada exitosamente para preservar su historial operativo y de auditoría.";
        };
        return new CloseStoreResponse(
                result.storeId(),
                result.action(),
                result.storeStatus(),
                message
        );
    }
}

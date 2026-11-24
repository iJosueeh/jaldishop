package com.jaldishop.backend.catalog.application.service;

import com.jaldishop.backend.catalog.domain.enums.ProductStatus;
import com.jaldishop.backend.catalog.domain.model.Product;
import com.jaldishop.backend.catalog.domain.model.ProductVariant;
import com.jaldishop.backend.catalog.domain.repository.ProductRepository;
import com.jaldishop.backend.catalog.domain.repository.ProductVariantRepository;
import com.jaldishop.backend.shared.exception.ResourceConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ManageProductVariantLifecycleService {

    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;

    public ManageProductVariantLifecycleService(
            ProductVariantRepository variantRepository,
            ProductRepository productRepository
    ) {
        this.variantRepository = variantRepository;
        this.productRepository = productRepository;
    }

    public void delete(UUID variantId, UUID productId, UUID storeId) {
        ProductVariant variant = variantRepository.lockByIdAndStoreIdForUpdate(variantId, storeId)
                .filter(v -> v.getProductId().equals(productId))
                .orElseThrow(() -> new ResourceNotFoundException("Variante", variantId.toString()));

        if (variantRepository.hasOperationalHistory(variant.getId())) {
            throw new ResourceConflictException(
                    "VARIANT_HAS_OPERATIONAL_HISTORY",
                    "Esta variante tiene ítems de órdenes o reservas registradas y no puede eliminarse permanentemente. Puedes desactivarla en su lugar.",
                    Map.of(
                            "suggestedAction", "DEACTIVATE",
                            "currentStatus", variant.getStatus().name(),
                            "blockingReason", "OPERATIONAL_HISTORY"
                    )
            );
        }

        Product product = productRepository.findByIdAndStoreId(productId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", productId.toString()));

        if (product.getStatus() == ProductStatus.ACTIVE
                && variantRepository.countActiveByProductIdAndIdNot(productId, variantId) == 0) {
            throw new ResourceConflictException(
                    "LAST_ACTIVE_VARIANT",
                    "No puedes eliminar la última variante activa de un producto activo. Desactiva el producto o crea otra variante primero.",
                    Map.of(
                            "suggestedAction", "DEACTIVATE",
                            "currentStatus", variant.getStatus().name(),
                            "blockingReason", "LAST_ACTIVE_VARIANT"
                    )
            );
        }

        variantRepository.deleteVariantAndSafeOrphans(variant.getId(), storeId);
    }

    public ProductVariant updateStatus(UUID variantId, UUID productId, UUID storeId, String status) {
        ProductVariant variant = variantRepository.findByIdAndStoreId(variantId, storeId)
                .filter(v -> v.getProductId().equals(productId))
                .orElseThrow(() -> new ResourceNotFoundException("Variante", variantId.toString()));

        if ("ACTIVE".equalsIgnoreCase(status)) {
            variant.activate();
        } else if ("INACTIVE".equalsIgnoreCase(status)) {
            Product product = productRepository.findByIdAndStoreId(productId, storeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Producto", productId.toString()));
            if (product.getStatus() == ProductStatus.ACTIVE
                    && variantRepository.countActiveByProductIdAndIdNot(productId, variantId) == 0) {
                throw new ResourceConflictException(
                        "LAST_ACTIVE_VARIANT",
                        "No puedes desactivar la última variante activa de un producto activo.",
                        Map.of("blockingReason", "LAST_ACTIVE_VARIANT")
                );
            }
            variant.deactivate();
        } else {
            throw new IllegalArgumentException("Estado de variante no válido: " + status);
        }

        return variantRepository.save(variant);
    }
}

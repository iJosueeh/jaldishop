package com.jaldishop.backend.catalog.application.service;

import com.jaldishop.backend.catalog.domain.model.Product;
import com.jaldishop.backend.catalog.domain.port.CatalogMediaCleaner;
import com.jaldishop.backend.catalog.domain.repository.ProductRepository;
import com.jaldishop.backend.shared.exception.ResourceConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ManageProductLifecycleService {

    private final ProductRepository productRepository;
    private final CatalogMediaCleaner mediaCleaner;

    public ManageProductLifecycleService(ProductRepository productRepository) {
        this(productRepository, null);
    }

    @Autowired
    public ManageProductLifecycleService(ProductRepository productRepository, CatalogMediaCleaner mediaCleaner) {
        this.productRepository = productRepository;
        this.mediaCleaner = mediaCleaner;
    }

    public void delete(UUID productId, UUID storeId) {
        Product product = productRepository.lockByIdAndStoreIdForUpdate(productId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", productId.toString()));

        if (productRepository.hasOperationalHistory(product.getId())) {
            throw new ResourceConflictException(
                    "PRODUCT_HAS_OPERATIONAL_HISTORY",
                    "Este producto tiene órdenes, reservas o reseñas registradas y no puede eliminarse permanentemente. Puedes desactivarlo para ocultarlo del catálogo.",
                    Map.of(
                            "suggestedAction", "DEACTIVATE",
                            "currentStatus", product.getStatus().name(),
                            "blockingReason", "OPERATIONAL_HISTORY"
                    )
            );
        }

        List<String> imageUrls = productRepository.findProductImageUrls(product.getId());
        productRepository.deleteProductAndSafeOrphans(product.getId(), storeId);
        if (mediaCleaner != null && imageUrls != null && !imageUrls.isEmpty()) {
            mediaCleaner.enqueueAssetDeletions(imageUrls);
        }
    }

    public Product updateStatus(UUID productId, UUID storeId, String status) {
        Product product = productRepository.findByIdAndStoreId(productId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", productId.toString()));

        if ("ACTIVE".equalsIgnoreCase(status)) {
            product.activate();
        } else if ("INACTIVE".equalsIgnoreCase(status)) {
            product.deactivate();
        } else {
            throw new IllegalArgumentException("Estado de producto no válido: " + status);
        }

        return productRepository.save(product);
    }
}

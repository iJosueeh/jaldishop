package com.jaldishop.backend.catalog.application;

import com.jaldishop.backend.catalog.domain.CategoryRepository;
import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.domain.ProductRepository;
import com.jaldishop.backend.catalog.domain.ProductStatus;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public UpdateProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public Product execute(UpdateProductCommand command) {
        Product product = productRepository.findByIdAndStoreId(command.productId(), command.storeId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado o no pertenece a la tienda"));

        if (command.name() == null || command.name().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del producto no puede estar vacío");
        }

        // Validar nueva categoría
        categoryRepository.findByIdAndStoreId(command.categoryId(), command.storeId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada o no pertenece a la tienda"));

        String resolvedSlug = (command.slug() != null && !command.slug().isBlank())
                ? SlugUtils.toSlug(command.slug())
                : SlugUtils.toSlug(command.name());

        if (productRepository.existsByStoreIdAndSlugAndIdNot(command.storeId(), resolvedSlug, command.productId())) {
            throw new ConflictException("PRODUCT_SLUG_ALREADY_EXISTS", "Ya existe un producto con el slug '" + resolvedSlug + "' en esta tienda");
        }

        product.update(
                command.categoryId(),
                command.name().trim(),
                resolvedSlug,
                command.description(),
                command.imageUrl()
        );

        if (command.status() != null && !command.status().isBlank()) {
            ProductStatus newStatus = ProductStatus.valueOf(command.status().trim().toUpperCase());
            if (newStatus == ProductStatus.ACTIVE) {
                product.activate();
            } else {
                product.deactivate();
            }
        }

        return productRepository.save(product);
    }
}

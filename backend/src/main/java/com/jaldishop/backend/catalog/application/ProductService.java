package com.jaldishop.backend.catalog.application;

import com.jaldishop.backend.catalog.domain.CategoryRepository;
import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.domain.ProductRepository;
import com.jaldishop.backend.catalog.domain.ProductStatus;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Product createProduct(CreateProductCommand command) {
        if (command.name() == null || command.name().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be empty");
        }

        // Validar pertenencia de Category a la Store
        categoryRepository.findByIdAndStoreId(command.categoryId(), command.storeId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found or does not belong to store"));

        // Resolver slug: si no viene explícito, se genera del nombre
        String resolvedSlug = (command.slug() != null && !command.slug().isBlank())
                ? SlugUtils.toSlug(command.slug())
                : SlugUtils.toSlug(command.name());

        if (productRepository.existsByStoreIdAndSlug(command.storeId(), resolvedSlug)) {
            throw new ConflictException("PRODUCT_SLUG_ALREADY_EXISTS", "A product with slug '" + resolvedSlug + "' already exists in this store");
        }

        Product product = Product.create(
                command.storeId(),
                command.categoryId(),
                command.name().trim(),
                resolvedSlug,
                command.description(),
                command.imageUrl()
        );

        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByStore(UUID storeId) {
        return productRepository.findByStoreId(storeId);
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsByStoreAndCategory(UUID storeId, UUID categoryId) {
        return productRepository.findByStoreIdAndCategoryId(storeId, categoryId);
    }

    @Transactional(readOnly = true)
    public Product getProductByIdAndStore(UUID productId, UUID storeId) {
        return productRepository.findByIdAndStoreId(productId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found or does not belong to store"));
    }

    @Transactional
    public Product updateProduct(UpdateProductCommand command) {
        Product product = getProductByIdAndStore(command.productId(), command.storeId());

        if (command.name() == null || command.name().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be empty");
        }

        // Validar nueva categoría
        categoryRepository.findByIdAndStoreId(command.categoryId(), command.storeId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found or does not belong to store"));

        String resolvedSlug = (command.slug() != null && !command.slug().isBlank())
                ? SlugUtils.toSlug(command.slug())
                : SlugUtils.toSlug(command.name());

        if (productRepository.existsByStoreIdAndSlugAndIdNot(command.storeId(), resolvedSlug, command.productId())) {
            throw new ConflictException("PRODUCT_SLUG_ALREADY_EXISTS", "A product with slug '" + resolvedSlug + "' already exists in this store");
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
package com.jaldishop.backend.catalog.application;

import com.jaldishop.backend.catalog.domain.CategoryRepository;
import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.domain.ProductRepository;
import com.jaldishop.backend.shared.exception.ConflictException;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public CreateProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public Product execute(CreateProductCommand command) {
        if (command.name() == null || command.name().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del producto no puede estar vacío");
        }

        // Validar pertenencia de Category a la Store
        categoryRepository.findByIdAndStoreId(command.categoryId(), command.storeId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada o no pertenece a la tienda"));

        // Resolver slug: si no viene explícito, se genera del nombre
        String resolvedSlug = (command.slug() != null && !command.slug().isBlank())
                ? SlugUtils.toSlug(command.slug())
                : SlugUtils.toSlug(command.name());

        if (productRepository.existsByStoreIdAndSlug(command.storeId(), resolvedSlug)) {
            throw new ConflictException("PRODUCT_SLUG_ALREADY_EXISTS", "Ya existe un producto con el slug '" + resolvedSlug + "' en esta tienda");
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
}

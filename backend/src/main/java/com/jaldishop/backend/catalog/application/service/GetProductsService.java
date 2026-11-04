package com.jaldishop.backend.catalog.application.service;
import com.jaldishop.backend.catalog.domain.model.*;
import com.jaldishop.backend.catalog.domain.enums.*;
import com.jaldishop.backend.catalog.domain.repository.*;
import com.jaldishop.backend.catalog.application.command.*;
import com.jaldishop.backend.catalog.application.query.*;
import com.jaldishop.backend.catalog.application.util.*;


import com.jaldishop.backend.catalog.domain.model.Product;
import com.jaldishop.backend.catalog.domain.repository.ProductRepository;
import com.jaldishop.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetProductsService {

    private final ProductRepository productRepository;

    public GetProductsService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> execute(GetProductsQuery query) {
        if (query.categoryId() != null) {
            return productRepository.findByStoreIdAndCategoryId(query.storeId(), query.categoryId());
        }
        return productRepository.findByStoreId(query.storeId());
    }

    public Product execute(UUID productId, UUID storeId) {
        return productRepository.findByIdAndStoreId(productId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado o no pertenece a la tienda"));
    }
}

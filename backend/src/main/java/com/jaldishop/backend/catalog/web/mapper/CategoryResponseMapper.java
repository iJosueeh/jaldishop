package com.jaldishop.backend.catalog.web.mapper;

import com.jaldishop.backend.catalog.domain.Category;
import com.jaldishop.backend.catalog.web.dto.CategoryResponse;
import org.springframework.stereotype.Component;

@Component
public class CategoryResponseMapper {

    public CategoryResponse toResponse(Category category) {
        if (category == null) {
            return null;
        }
        return new CategoryResponse(
                category.getId(),
                category.getStoreId(),
                category.getName(),
                category.getDescription(),
                category.getStatus().name(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}

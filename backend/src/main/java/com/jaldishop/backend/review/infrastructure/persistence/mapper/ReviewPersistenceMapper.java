package com.jaldishop.backend.review.infrastructure.persistence.mapper;

import com.jaldishop.backend.review.domain.Review;
import com.jaldishop.backend.review.domain.ReviewStatus;
import com.jaldishop.backend.review.infrastructure.persistence.entity.ReviewEntity;
import org.springframework.stereotype.Component;

@Component
public class ReviewPersistenceMapper {

    public Review toDomain(ReviewEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Review(
                entity.getId(),
                entity.getUserId(),
                entity.getProductId(),
                entity.getRating(),
                entity.getComment(),
                ReviewStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public ReviewEntity toEntity(Review domain) {
        if (domain == null) {
            return null;
        }
        return new ReviewEntity(
                domain.getId(),
                domain.getUserId(),
                domain.getProductId(),
                domain.getRating(),
                domain.getComment(),
                domain.getStatus().name(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}


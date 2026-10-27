package com.jaldishop.backend.review.infrastructure.persistence.adapter;

import com.jaldishop.backend.review.domain.Review;
import com.jaldishop.backend.review.domain.ReviewRepository;
import com.jaldishop.backend.review.domain.ReviewStatus;
import com.jaldishop.backend.review.infrastructure.persistence.entity.ReviewEntity;
import com.jaldishop.backend.review.infrastructure.persistence.mapper.ReviewPersistenceMapper;
import com.jaldishop.backend.review.infrastructure.persistence.repository.ReviewJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaReviewRepositoryAdapter implements ReviewRepository {

    private final ReviewJpaRepository jpaRepository;
    private final ReviewPersistenceMapper mapper;

    public JpaReviewRepositoryAdapter(ReviewJpaRepository jpaRepository, ReviewPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Review save(Review review) {
        ReviewEntity entity = mapper.toEntity(review);
        ReviewEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Review> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Review> findByUserIdAndProductId(UUID userId, UUID productId) {
        return jpaRepository.findByUserIdAndProductId(userId, productId).map(mapper::toDomain);
    }

    @Override
    public List<Review> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Review> findByProductIdAndStatus(UUID productId, ReviewStatus status) {
        return jpaRepository.findByProductIdAndStatus(productId, status.name()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByUserIdAndProductId(UUID userId, UUID productId) {
        return jpaRepository.existsByUserIdAndProductId(userId, productId);
    }
}

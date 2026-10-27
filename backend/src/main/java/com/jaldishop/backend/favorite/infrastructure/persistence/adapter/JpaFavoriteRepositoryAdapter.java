package com.jaldishop.backend.favorite.infrastructure.persistence.adapter;

import com.jaldishop.backend.favorite.domain.Favorite;
import com.jaldishop.backend.favorite.domain.FavoriteRepository;
import com.jaldishop.backend.favorite.infrastructure.persistence.entity.FavoriteEntity;
import com.jaldishop.backend.favorite.infrastructure.persistence.entity.FavoriteId;
import com.jaldishop.backend.favorite.infrastructure.persistence.mapper.FavoritePersistenceMapper;
import com.jaldishop.backend.favorite.infrastructure.persistence.repository.FavoriteJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaFavoriteRepositoryAdapter implements FavoriteRepository {

    private final FavoriteJpaRepository jpaRepository;
    private final FavoritePersistenceMapper mapper;

    public JpaFavoriteRepositoryAdapter(FavoriteJpaRepository jpaRepository,
                                        FavoritePersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Favorite save(Favorite favorite) {
        FavoriteEntity entity = mapper.toEntity(favorite);
        FavoriteEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public void delete(UUID userId, UUID productId) {
        FavoriteId id = new FavoriteId(userId, productId);
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean exists(UUID userId, UUID productId) {
        return jpaRepository.existsByUserIdAndProductId(userId, productId);
    }

    @Override
    public Optional<Favorite> findByUserIdAndProductId(UUID userId, UUID productId) {
        FavoriteId id = new FavoriteId(userId, productId);
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Favorite> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}

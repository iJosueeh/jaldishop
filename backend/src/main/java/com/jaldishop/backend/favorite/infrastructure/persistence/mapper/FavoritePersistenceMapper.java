package com.jaldishop.backend.favorite.infrastructure.persistence.mapper;

import com.jaldishop.backend.favorite.domain.Favorite;
import com.jaldishop.backend.favorite.infrastructure.persistence.entity.FavoriteEntity;
import org.springframework.stereotype.Component;

@Component
public class FavoritePersistenceMapper {

    public Favorite toDomain(FavoriteEntity entity) {
        if (entity == null || entity.getId() == null) {
            return null;
        }
        return new Favorite(
                entity.getId().getUserId(),
                entity.getId().getProductId(),
                entity.getCreatedAt()
        );
    }

    public FavoriteEntity toEntity(Favorite domain) {
        if (domain == null) {
            return null;
        }
        return new FavoriteEntity(
                domain.getUserId(),
                domain.getProductId(),
                domain.getCreatedAt()
        );
    }
}

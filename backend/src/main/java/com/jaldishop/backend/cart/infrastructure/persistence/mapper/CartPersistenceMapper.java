package com.jaldishop.backend.cart.infrastructure.persistence.mapper;

import com.jaldishop.backend.cart.domain.Cart;
import com.jaldishop.backend.cart.domain.CartItem;
import com.jaldishop.backend.cart.infrastructure.persistence.entity.CartEntity;
import com.jaldishop.backend.cart.infrastructure.persistence.entity.CartItemEntity;
import com.jaldishop.backend.cart.infrastructure.persistence.entity.CartItemId;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CartPersistenceMapper {

    public Cart toDomain(CartEntity entity) {
        if (entity == null) {
            return null;
        }

        List<CartItem> items = entity.getItems() != null
                ? entity.getItems().stream().map(this::toItemDomain).collect(Collectors.toList())
                : new ArrayList<>();

        return new Cart(
                entity.getId(),
                entity.getUserId(),
                entity.getStoreId(),
                items,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public CartEntity toEntity(Cart domain) {
        if (domain == null) {
            return null;
        }

        CartEntity entity = new CartEntity(
                domain.getId(),
                domain.getUserId(),
                domain.getStoreId(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );

        if (domain.getItems() != null) {
            List<CartItemEntity> itemEntities = domain.getItems().stream()
                    .map(item -> toItemEntity(item, entity))
                    .collect(Collectors.toList());
            entity.setItems(itemEntities);
        }

        return entity;
    }

    public CartItem toItemDomain(CartItemEntity entity) {
        if (entity == null) {
            return null;
        }
        return new CartItem(
                entity.getCartId(),
                entity.getVariantId(),
                entity.getQuantity(),
                entity.getReferencePriceAmount(),
                entity.getReferencePriceCurrency(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public CartItemEntity toItemEntity(CartItem domain, CartEntity cartEntity) {
        if (domain == null) {
            return null;
        }
        CartItemId id = new CartItemId(domain.getCartId(), domain.getVariantId());
        return new CartItemEntity(
                id,
                cartEntity,
                domain.getQuantity(),
                domain.getReferencePriceAmount(),
                domain.getReferencePriceCurrency(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}

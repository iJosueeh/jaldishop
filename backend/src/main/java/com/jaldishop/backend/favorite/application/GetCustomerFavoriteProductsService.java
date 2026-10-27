package com.jaldishop.backend.favorite.application;

import com.jaldishop.backend.catalog.domain.Product;
import com.jaldishop.backend.catalog.domain.ProductRepository;
import com.jaldishop.backend.catalog.domain.ProductStatus;
import com.jaldishop.backend.catalog.domain.ProductVariant;
import com.jaldishop.backend.catalog.domain.ProductVariantRepository;
import com.jaldishop.backend.catalog.domain.VariantStatus;
import com.jaldishop.backend.favorite.domain.Favorite;
import com.jaldishop.backend.favorite.domain.FavoriteRepository;
import com.jaldishop.backend.favorite.web.dto.FavoriteProductResponse;
import com.jaldishop.backend.store.domain.Store;
import com.jaldishop.backend.store.domain.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class GetCustomerFavoriteProductsService {

    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final StoreRepository storeRepository;

    public GetCustomerFavoriteProductsService(FavoriteRepository favoriteRepository,
                                             ProductRepository productRepository,
                                             ProductVariantRepository productVariantRepository,
                                             StoreRepository storeRepository) {
        this.favoriteRepository = favoriteRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.storeRepository = storeRepository;
    }

    @Transactional(readOnly = true)
    public List<FavoriteProductResponse> execute(UUID userId) {
        Objects.requireNonNull(userId, "El identificador del cliente es obligatorio.");

        List<Favorite> favorites = favoriteRepository.findByUserId(userId);
        if (favorites.isEmpty()) {
            return Collections.emptyList();
        }

        List<FavoriteProductResponse> results = new ArrayList<>();

        for (Favorite fav : favorites) {
            Optional<Product> productOpt = productRepository.findById(fav.getProductId());
            if (productOpt.isEmpty()) {
                continue;
            }

            Product product = productOpt.get();
            Optional<Store> storeOpt = storeRepository.findById(product.getStoreId());
            String storeName = storeOpt.map(Store::getName).orElse("Comercio local");
            String storeSlug = storeOpt.map(Store::getSlug).orElse("");

            List<ProductVariant> variants = productVariantRepository.findByProductId(product.getId());

            BigDecimal minPrice = variants.stream()
                    .filter(v -> v.getStatus() == VariantStatus.ACTIVE)
                    .map(ProductVariant::getPriceAmount)
                    .min(Comparator.naturalOrder())
                    .orElse(BigDecimal.ZERO);

            String currency = variants.isEmpty() ? "PEN" : variants.getFirst().getPriceCurrency();

            boolean isAvailable = product.getStatus() == ProductStatus.ACTIVE &&
                    variants.stream().anyMatch(v -> v.getStatus() == VariantStatus.ACTIVE);

            results.add(new FavoriteProductResponse(
                    product.getId(),
                    product.getName(),
                    product.getSlug(),
                    product.getImageUrl(),
                    product.getStoreId(),
                    storeName,
                    storeSlug,
                    minPrice,
                    currency,
                    isAvailable,
                    fav.getCreatedAt()
            ));
        }

        return results;
    }
}

'use client';

import React from 'react';
import { ProductCard } from './ProductCard';
import { CatalogEmptyState } from './CatalogEmptyState';
import { CartItem, ProductItem } from '../../types/storefront.types';

interface ProductGridProps {
  storeSlug?: string;
  products: ProductItem[];
  onAdd: (product: ProductItem) => void;
  cart: CartItem[];
  storeName: string;
  onResetFilters: () => void;
  isFiltering?: boolean;
  pending?: boolean;
  isFavorite?: (productId: string) => boolean;
  onToggleFavorite?: (productId: string) => void;
  isTogglingFavorite?: boolean;
}

export function ProductGrid({
  storeSlug,
  products,
  onAdd,
  cart,
  storeName,
  onResetFilters,
  isFiltering = false,
  pending = false,
  isFavorite,
  onToggleFavorite,
  isTogglingFavorite = false,
}: ProductGridProps) {
  if (products.length === 0) {
    return (
      <CatalogEmptyState
        type={isFiltering ? 'no-results' : 'no-products'}
        storeName={storeName}
        onReset={onResetFilters}
      />
    );
  }

  // Mapa de cantidad en carrito por producto
  const quantities = new Map<string, number>();
  cart.forEach(({ product, quantity }) => quantities.set(product.id, (quantities.get(product.id) || 0) + quantity));

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5 gap-4 sm:gap-5">
      {products.map((product) => (
        <ProductCard
          key={product.id}
          product={product}
          storeSlug={storeSlug}
          onAdd={onAdd}
          currentQuantity={quantities.get(product.id) || 0}
          pending={pending}
          isFavorite={isFavorite ? isFavorite(product.id) : false}
          onToggleFavorite={onToggleFavorite}
          isTogglingFavorite={isTogglingFavorite}
        />
      ))}
    </div>
  );
}

'use client';

import React from 'react';
import { ProductCard } from './ProductCard';
import { CatalogEmptyState } from './CatalogEmptyState';
import { CartItem, ProductItem } from '../../types/storefront.types';

interface ProductGridProps {
  products: ProductItem[];
  onAdd: (product: ProductItem) => void;
  cart: CartItem[];
  storeName: string;
  onResetFilters: () => void;
  isFiltering?: boolean;
  pending?: boolean;
}

export function ProductGrid({
  products,
  onAdd,
  cart,
  storeName,
  onResetFilters,
  isFiltering = false,
  pending = false,
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
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5 sm:gap-6">
      {products.map((product) => (
        <ProductCard
          key={product.id}
          product={product}
          onAdd={onAdd}
          currentQuantity={quantities.get(product.id) || 0}
          pending={pending}
        />
      ))}
    </div>
  );
}

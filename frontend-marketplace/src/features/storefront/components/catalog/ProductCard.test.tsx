import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ProductCard } from './ProductCard';
import { ProductGrid } from './ProductGrid';
import { CartSummarySidebar } from './CartSummarySidebar';
import { ProductItem, CartItem } from '../../types/storefront.types';

describe('Modular Catalog Components', () => {
  const singlePriceProduct: ProductItem = {
    id: 'prod-1',
    name: 'Torta Selva Negra',
    description: 'Bizcocho de chocolate relleno con crema chantilly y cerezas.',
    price: 45.0,
    category: 'Tortas',
    imageUrl: 'https://images.unsplash.com/photo-cake.jpg',
    prepTimeMinutes: 20,
    variants: [{ id: 'v-cake', presentationName: 'Unidad', priceAmount: 45, priceCurrency: 'PEN', tracksInventory: false, status: 'ACTIVE' }],
  };

  const rangePriceProduct: ProductItem = {
    id: 'prod-2',
    name: 'Empanadas Artesanales',
    description: 'Masa hojaldrada horneada.',
    price: 8.5,
    minPrice: 8.5,
    maxPrice: 24.0,
    category: 'Salados',
    variants: [
      { id: 'v1', presentationName: 'Unidad', priceAmount: 8.5, priceCurrency: 'PEN', tracksInventory: false, status: 'ACTIVE' },
      { id: 'v2', presentationName: 'Caja x3', priceAmount: 24.0, priceCurrency: 'PEN', tracksInventory: false, status: 'ACTIVE' },
    ],
  };

  it('ProductCard renders product title, description, and single price in soles', () => {
    const onAdd = vi.fn();
    render(<ProductCard product={singlePriceProduct} onAdd={onAdd} />);

    expect(screen.getByText('Torta Selva Negra')).toBeInTheDocument();
    expect(screen.getByText(/Bizcocho de chocolate/)).toBeInTheDocument();
    expect(screen.getByText('S/ 45.00')).toBeInTheDocument();
    expect(screen.getByText('Preparación ~20 min')).toBeInTheDocument();

    const addBtn = screen.getByRole('button', { name: /Añadir/i });
    fireEvent.click(addBtn);
    expect(onAdd).toHaveBeenCalledWith({ ...singlePriceProduct, variantId: 'v-cake', price: 45 });
  });

  it('ProductCard displays price range and formats badge when multiple variants exist', () => {
    const onAdd = vi.fn();
    render(<ProductCard product={rangePriceProduct} onAdd={onAdd} currentQuantity={2} />);

    expect(screen.getByText('Empanadas Artesanales')).toBeInTheDocument();
    expect(screen.getByText('S/ 8.50 - S/ 24.00')).toBeInTheDocument();
    expect(screen.getByText('Formatos')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Añadir \(2\)/i })).toBeDisabled();
    fireEvent.change(screen.getByLabelText('Presentación de Empanadas Artesanales'), { target: { value: 'v2' } });
    fireEvent.click(screen.getByRole('button', { name: /Añadir \(2\)/i }));
    expect(onAdd).toHaveBeenCalledWith(expect.objectContaining({ variantId: 'v2', price: 24 }));
  });

  it('ProductGrid renders multiple cards or empty search state', () => {
    const onAdd = vi.fn();
    const onReset = vi.fn();

    const { rerender } = render(
      <ProductGrid
        products={[singlePriceProduct, rangePriceProduct]}
        onAdd={onAdd}
        cart={[]}
        storeName="Pastelería Pepe"
        onResetFilters={onReset}
      />
    );

    expect(screen.getByText('Torta Selva Negra')).toBeInTheDocument();
    expect(screen.getByText('Empanadas Artesanales')).toBeInTheDocument();

    rerender(
      <ProductGrid
        products={[]}
        onAdd={onAdd}
        cart={[]}
        storeName="Pastelería Pepe"
        onResetFilters={onReset}
        isFiltering={true}
      />
    );

    expect(screen.getByText('No hay productos que coincidan con tu búsqueda')).toBeInTheDocument();
    const resetBtn = screen.getByText('Ver todos los productos');
    fireEvent.click(resetBtn);
    expect(onReset).toHaveBeenCalled();
  });

  it('CartSummarySidebar shows empty placeholder when cart is empty and items when present', () => {
    const onChangeQuantity = vi.fn();

    const { rerender } = render(
      <CartSummarySidebar
        cart={[]}
        storeName="Pastelería Pepe"
        subtotal={0}
        onChangeQuantity={onChangeQuantity}
      />
    );

    expect(screen.getByText(/Añade productos de Pastelería Pepe/)).toBeInTheDocument();

    const cart: CartItem[] = [
      { product: singlePriceProduct, quantity: 2 },
    ];

    rerender(
      <CartSummarySidebar
        cart={cart}
        storeName="Pastelería Pepe"
        subtotal={90.0}
        onChangeQuantity={onChangeQuantity}
      />
    );

    expect(screen.getByText('Torta Selva Negra')).toBeInTheDocument();
    expect(screen.getAllByText('S/ 90.00')).toHaveLength(2);
    expect(screen.getByRole('button', { name: 'Compra en línea no disponible' })).toBeDisabled();
  });
});

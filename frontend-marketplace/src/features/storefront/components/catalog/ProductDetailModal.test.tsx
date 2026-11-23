import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import React from 'react';
import { ProductDetailModal } from './ProductDetailModal';
import { ProductItem } from '../../types/storefront.types';

const mockProduct: ProductItem = {
  id: 'prod-123',
  name: 'Pan Campesino de Masa Madre',
  description: 'Elaborado con harina integral orgánica y fermentación lenta de 24 horas.',
  price: 15.0,
  category: 'Panadería',
  prepTimeMinutes: 25,
  variants: [
    {
      id: 'var-1',
      presentationName: 'Unidad 500g',
      priceAmount: 15.0,
      priceCurrency: 'PEN',
      tracksInventory: true,
      status: 'ACTIVE',
    },
    {
      id: 'var-2',
      presentationName: 'Hogaza Familiar 1kg',
      priceAmount: 26.0,
      priceCurrency: 'PEN',
      tracksInventory: true,
      status: 'ACTIVE',
    },
  ],
};

describe('ProductDetailModal Component', () => {
  it('does not render when isOpen is false', () => {
    const { container } = render(
      <ProductDetailModal
        isOpen={false}
        onClose={vi.fn()}
        product={mockProduct}
        onAdd={vi.fn()}
      />
    );
    expect(container.firstChild).toBeNull();
  });

  it('renders product information, full description and price when open', () => {
    render(
      <ProductDetailModal
        isOpen={true}
        onClose={vi.fn()}
        product={mockProduct}
        onAdd={vi.fn()}
      />
    );

    expect(screen.getByText('Pan Campesino de Masa Madre')).toBeInTheDocument();
    expect(
      screen.getByText(/Elaborado con harina integral orgánica y fermentación lenta/i)
    ).toBeInTheDocument();
    expect(screen.getByText('Panadería')).toBeInTheDocument();
    expect(screen.getByText('~25 min')).toBeInTheDocument();
    expect(screen.getByText('Unidad 500g')).toBeInTheDocument();
    expect(screen.getByText('Hogaza Familiar 1kg')).toBeInTheDocument();
  });

  it('updates price and adds variant to cart on submit', () => {
    const onAddMock = vi.fn();
    const onCloseMock = vi.fn();

    render(
      <ProductDetailModal
        isOpen={true}
        onClose={onCloseMock}
        product={mockProduct}
        onAdd={onAddMock}
      />
    );

    // Click second variant
    const variant2Btn = screen.getByText('Hogaza Familiar 1kg');
    fireEvent.click(variant2Btn);

    // Increase quantity to 2
    const plusBtn = screen.getByLabelText(/Aumentar cantidad/i);
    fireEvent.click(plusBtn);

    // Submit add to cart
    const addBtn = screen.getByRole('button', { name: /Añadir al carrito/i });
    fireEvent.click(addBtn);

    expect(onAddMock).toHaveBeenCalledWith(
      expect.objectContaining({
        id: 'prod-123',
        variantId: 'var-2',
        price: 26.0,
        name: 'Pan Campesino de Masa Madre — Hogaza Familiar 1kg',
      }),
      2
    );
    expect(onCloseMock).toHaveBeenCalled();
  });

  it('calls onToggleFavorite when heart button is clicked', () => {
    const toggleMock = vi.fn();

    render(
      <ProductDetailModal
        isOpen={true}
        onClose={vi.fn()}
        product={mockProduct}
        onAdd={vi.fn()}
        isFavorite={false}
        onToggleFavorite={toggleMock}
      />
    );

    const favBtn = screen.getByLabelText(/Añadir a favoritos/i);
    fireEvent.click(favBtn);

    expect(toggleMock).toHaveBeenCalledTimes(1);
  });
});

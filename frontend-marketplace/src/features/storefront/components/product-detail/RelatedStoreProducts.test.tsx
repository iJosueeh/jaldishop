import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { RelatedStoreProducts } from './RelatedStoreProducts';
import { ProductItem, PublicStore } from '../../types/storefront.types';

const mockSelectStore = vi.fn();
const mockChangeQuantity = vi.fn().mockResolvedValue(undefined);

vi.mock('../../cart/StorefrontCartProvider', () => ({
  useStorefrontCart: () => ({
    items: [],
    selectStore: mockSelectStore,
    changeQuantity: mockChangeQuantity,
    pending: false,
  }),
}));

vi.mock('sonner', () => ({
  toast: { success: vi.fn(), error: vi.fn() },
}));

describe('RelatedStoreProducts', () => {
  const store: PublicStore = {
    id: 'store-1',
    name: 'Panadería Pepe',
    slug: 'panaderia-pepe',
    status: 'ACTIVE',
    deliveryEnabled: true,
    pickupEnabled: true,
  };

  const related: ProductItem[] = [
    {
      id: 'prod-2',
      slug: 'croissant-mantequilla',
      name: 'Croissant de Mantequilla',
      description: 'Hojaldre artesanal',
      price: 8,
      category: 'Panes',
      variants: [
        {
          id: 'v-20',
          presentationName: 'Unidad',
          priceAmount: 8,
          priceCurrency: 'PEN',
          tracksInventory: false,
          status: 'ACTIVE',
        },
      ],
    },
  ];

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders related products and adds to cart using store context', async () => {
    render(<RelatedStoreProducts store={store} products={related} />);

    expect(screen.getByRole('heading', { name: /Productos relacionados/i })).toBeInTheDocument();
    expect(screen.getByText('Croissant de Mantequilla')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Añadir' }));

    await waitFor(() => {
      expect(mockSelectStore).toHaveBeenCalledWith({
        id: 'store-1',
        name: 'Panadería Pepe',
        slug: 'panaderia-pepe',
      });
      expect(mockChangeQuantity).toHaveBeenCalledWith(
        expect.objectContaining({ id: 'prod-2', variantId: 'v-20', price: 8 }),
        1
      );
    });
  });

  it('renders nothing when related list is empty', () => {
    const { container } = render(<RelatedStoreProducts store={store} products={[]} />);
    expect(container.firstChild).toBeNull();
  });
});

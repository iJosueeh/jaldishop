import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ProductDetailView } from './ProductDetailView';
import { ProductItem, PublicStore } from '../types/storefront.types';

const mockSelectStore = vi.fn();
const mockChangeQuantity = vi.fn().mockResolvedValue(undefined);

vi.mock('../cart/StorefrontCartProvider', () => ({
  useStorefrontCart: () => ({
    selectStore: mockSelectStore,
    changeQuantity: mockChangeQuantity,
    pending: false,
  }),
}));

vi.mock('./FavoriteButton', () => ({
  FavoriteButton: ({ productId }: { productId: string }) => (
    <button type="button" aria-label={`Favorito ${productId}`}>
      Fav
    </button>
  ),
}));

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

describe('ProductDetailView (Marketplace)', () => {
  const store: PublicStore = {
    id: 'store-1',
    name: 'Pastelería Pepe',
    slug: 'pasteleria-pepe',
    category: 'Pastelería',
    deliveryEnabled: true,
    pickupEnabled: true,
  };

  const product: ProductItem = {
    id: 'prod-1',
    slug: 'torta-selva-negra',
    name: 'Torta Selva Negra',
    description: 'Bizcocho artesanal con cerezas.',
    price: 45,
    category: 'Tortas',
    prepTimeMinutes: 25,
    variants: [
      {
        id: 'v-1',
        presentationName: 'Mediana 8 porciones',
        priceAmount: 45,
        priceCurrency: 'PEN',
        tracksInventory: true,
        status: 'ACTIVE',
        attributes: [{ name: 'Porciones', value: '8' }],
      },
      {
        id: 'v-2',
        presentationName: 'Grande 14 porciones',
        priceAmount: 75,
        priceCurrency: 'PEN',
        tracksInventory: true,
        status: 'ACTIVE',
        attributes: [{ name: 'Porciones', value: '14' }],
      },
    ],
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders breadcrumb, specifications, reviews and allows selecting variant and quantity', async () => {
    render(
      <ProductDetailView
        store={store}
        product={product}
        storeSettings={{
          cancellationPolicy: 'Cancelación gratis hasta 2 horas antes.',
          refundPolicy: 'Reembolso completo.',
          requireCustomerDni: false,
          allowOrderNotes: true,
          onlinePaymentsEnabled: true,
          cashOnDeliveryEnabled: true,
          acceptsPosOnDelivery: false,
          minDeliveryAmount: null,
          estimatedPrepTimeMinutes: 30,
        }}
        reviewsSummary={{
          productId: 'prod-1',
          averageRating: 4.8,
          totalReviews: 1,
          reviews: [
            {
              id: 'rev-1',
              customerName: 'Ana P.',
              rating: 5,
              comment: 'Excelente sabor.',
              createdAt: '2026-11-20T10:00:00Z',
            },
          ],
        }}
      />
    );

    expect(screen.getByRole('heading', { name: 'Torta Selva Negra' })).toBeInTheDocument();
    expect(screen.getByText('Porciones')).toBeInTheDocument();
    expect(screen.getByText('8')).toBeInTheDocument();
    expect(screen.getByText('Excelente sabor.')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /Grande 14 porciones/i }));
    expect(screen.getByText('14')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Aumentar cantidad' }));
    fireEvent.click(screen.getByRole('button', { name: /Añadir al carrito/i }));

    await waitFor(() => {
      expect(mockSelectStore).toHaveBeenCalledWith({
        id: 'store-1',
        name: 'Pastelería Pepe',
        slug: 'pasteleria-pepe',
      });
      expect(mockChangeQuantity).toHaveBeenCalledWith(
        expect.objectContaining({
          id: 'prod-1',
          variantId: 'v-2',
          price: 75,
        }),
        2
      );
    });
  });
});

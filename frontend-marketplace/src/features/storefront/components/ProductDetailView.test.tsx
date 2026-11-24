import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ProductDetailView } from './ProductDetailView';
import { ProductItem, PublicStore } from '../types/storefront.types';
import { storeService } from '../services/storeService';
import { ORDER_COMPLETED_EVENT } from '../hooks/useProductLiveSync';

const mockSelectStore = vi.fn();
const mockChangeQuantity = vi.fn().mockResolvedValue(undefined);

vi.mock('../cart/StorefrontCartProvider', () => ({
  useStorefrontCart: () => ({
    items: [
      {
        quantity: 1,
        product: {
          id: 'prod-1',
          variantId: 'v-1',
          name: 'Torta Selva Negra — Mediana 8 porciones',
          description: '',
          price: 45,
          category: 'Tortas',
        },
      },
    ],
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

vi.mock('@/features/customer/hooks/useReviewEligibility', () => ({
  useReviewEligibility: () => ({
    data: { productId: 'prod-1', eligible: false, alreadyReviewed: false, currentReview: null },
    isLoading: false,
    isError: false,
    error: null,
  }),
}));

vi.mock('@/features/customer/hooks/useCustomerReviews', () => ({
  useCustomerReviews: () => ({
    deleteReview: vi.fn(),
    isDeleting: false,
    createReview: vi.fn(),
    updateReview: vi.fn(),
  }),
}));

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

function renderWithQuery(ui: React.ReactElement) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe('ProductDetailView (Marketplace)', () => {
  const store: PublicStore = {
    id: 'store-1',
    name: 'Pastelería Pepe',
    slug: 'pasteleria-pepe',
    category: 'Pastelería',
    status: 'ACTIVE',
    deliveryEnabled: true,
    pickupEnabled: true,
    whatsappNumber: '51999111222',
    deliveryFeeAmount: 8,
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
        sku: 'TSN-MED-08',
        priceAmount: 45,
        priceCurrency: 'PEN',
        tracksInventory: true,
        availableQuantity: 10,
        status: 'ACTIVE',
        attributes: [{ name: 'Porciones', value: '8' }],
      },
      {
        id: 'v-2',
        presentationName: 'Grande 14 porciones',
        sku: 'TSN-GDE-14',
        priceAmount: 75,
        priceCurrency: 'PEN',
        tracksInventory: true,
        availableQuantity: 2,
        status: 'ACTIVE',
        attributes: [{ name: 'Porciones', value: '14' }],
      },
    ],
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('updates SKU, attributes, stock and clamps quantity when switching variants', async () => {
    renderWithQuery(
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
    expect(screen.queryByText(/SKU:/i)).not.toBeInTheDocument();
    expect(screen.getAllByText('10 disponibles').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('Pago en línea · Efectivo')).toBeInTheDocument();
    expect(screen.getByText(/Ya tienes 1 unidad en tu carrito/i)).toBeInTheDocument();

    // Aumentar cantidad a 4 en variante 1 (stock 10)
    const increaseBtn = screen.getByRole('button', { name: 'Aumentar cantidad' });
    fireEvent.click(increaseBtn);
    fireEvent.click(increaseBtn);
    fireEvent.click(increaseBtn);

    // Cambiar a variante 2 (stock 2) -> debe hacer auto-clamp de 4 a 2
    fireEvent.click(screen.getByRole('radio', { name: /Grande 14 porciones/i }));
    expect(screen.getAllByText('¡Últimas 2 unidades!').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('14')).toBeInTheDocument();

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

  it('refreshes stock without page reload when an order completes', async () => {
    const updatedProduct: ProductItem = {
      ...product,
      variants: [
        {
          ...product.variants![0],
          availableQuantity: 0,
        },
        product.variants![1],
      ],
    };

    const liveSpy = vi
      .spyOn(storeService, 'getStoreProductBySlugLive')
      .mockResolvedValueOnce(product)
      .mockResolvedValueOnce(updatedProduct);

    renderWithQuery(<ProductDetailView store={store} product={product} />);

    expect(screen.getByText('10 disponibles')).toBeInTheDocument();

    await act(async () => {
      window.dispatchEvent(new CustomEvent(ORDER_COMPLETED_EVENT));
    });

    await waitFor(() => {
      expect(liveSpy).toHaveBeenCalled();
      expect(screen.getByRole('button', { name: /Producto agotado/i })).toBeDisabled();
    });
  });

  it('renders editorial fallback when imageUrl is missing', () => {
    renderWithQuery(<ProductDetailView store={store} product={product} />);
    expect(screen.getByText('Fotografía no publicada por el comercio')).toBeInTheDocument();
  });
});

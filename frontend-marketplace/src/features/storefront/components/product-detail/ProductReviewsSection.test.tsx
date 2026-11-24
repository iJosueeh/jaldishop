import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ProductReviewsSection } from './ProductReviewsSection';
import { ProductReviewsSummary } from '../../types/storefront.types';

const mockUseReviewEligibility = vi.fn();
const mockDeleteReview = vi.fn().mockResolvedValue(undefined);

vi.mock('next/navigation', () => ({
  usePathname: () => '/tienda/mi-tienda/producto/mi-producto',
}));

vi.mock('@/features/customer/hooks/useReviewEligibility', () => ({
  useReviewEligibility: (productId: string) => mockUseReviewEligibility(productId),
}));

vi.mock('@/features/customer/hooks/useCustomerReviews', () => ({
  useCustomerReviews: () => ({
    deleteReview: mockDeleteReview,
    isDeleting: false,
    createReview: vi.fn().mockResolvedValue({}),
    updateReview: vi.fn().mockResolvedValue({}),
  }),
}));

vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

describe('ProductReviewsSection', () => {
  const summary: ProductReviewsSummary = {
    productId: 'prod-1',
    averageRating: 4.5,
    totalReviews: 2,
    reviews: [
      {
        id: 'r-1',
        customerName: 'Carlos M.',
        rating: 5,
        comment: 'Muy bueno',
        createdAt: '2026-11-21T10:00:00Z',
      },
      {
        id: 'r-2',
        customerName: 'Lucía R.',
        rating: 4,
        comment: 'Bien presentado',
        createdAt: '2026-11-20T10:00:00Z',
      },
    ],
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows login prompt when user is unauthenticated', () => {
    mockUseReviewEligibility.mockReturnValue({
      isLoading: false,
      isError: true,
      error: new Error('No autenticado.'),
      data: undefined,
    });

    render(
      <ProductReviewsSection
        productId="prod-1"
        productName="Producto Test"
        reviewsSummary={summary}
      />
    );

    const loginLink = screen.getByRole('link', { name: /Iniciar sesión para opinar/i });
    expect(loginLink).toHaveAttribute(
      'href',
      '/login?redirect=%2Ftienda%2Fmi-tienda%2Fproducto%2Fmi-producto'
    );
  });

  it('blocks review creation when user has no completed order', () => {
    mockUseReviewEligibility.mockReturnValue({
      isLoading: false,
      isError: false,
      error: null,
      data: { productId: 'prod-1', eligible: false, alreadyReviewed: false, currentReview: null },
    });

    render(
      <ProductReviewsSection
        productId="prod-1"
        productName="Producto Test"
        reviewsSummary={summary}
      />
    );

    expect(screen.queryByRole('button', { name: /Escribir mi reseña/i })).not.toBeInTheDocument();
    expect(screen.getByText(/completado exitosamente/i)).toBeInTheDocument();
  });

  it('allows creating a review when eligible and not reviewed yet', () => {
    mockUseReviewEligibility.mockReturnValue({
      isLoading: false,
      isError: false,
      error: null,
      data: { productId: 'prod-1', eligible: true, alreadyReviewed: false, currentReview: null },
    });

    render(
      <ProductReviewsSection
        productId="prod-1"
        productName="Producto Test"
        reviewsSummary={summary}
      />
    );

    const writeBtn = screen.getByRole('button', { name: /Escribir mi reseña/i });
    fireEvent.click(writeBtn);
    expect(screen.getByRole('dialog')).toBeInTheDocument();
  });

  it('shows published review with edit and delete when alreadyReviewed is true', async () => {
    const onReviewsChanged = vi.fn();
    mockUseReviewEligibility.mockReturnValue({
      isLoading: false,
      isError: false,
      error: null,
      data: {
        productId: 'prod-1',
        eligible: true,
        alreadyReviewed: true,
        currentReview: {
          id: 'r-1',
          productId: 'prod-1',
          productName: 'Producto Test',
          productSlug: 'producto-test',
          storeName: 'Tienda',
          storeSlug: 'tienda',
          imageUrl: null,
          rating: 5,
          comment: 'Excelente',
          status: 'PUBLISHED',
          createdAt: '2026-11-21T10:00:00Z',
          updatedAt: '2026-11-21T10:00:00Z',
        },
      },
    });

    render(
      <ProductReviewsSection
        productId="prod-1"
        productName="Producto Test"
        reviewsSummary={summary}
        onReviewsChanged={onReviewsChanged}
      />
    );

    expect(screen.queryByRole('button', { name: /Escribir mi reseña/i })).not.toBeInTheDocument();
    expect(screen.getByText(/Tu opinión publicada \(1 por producto\)/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /Eliminar/i }));
    fireEvent.click(screen.getByRole('button', { name: /Confirmar/i }));

    await waitFor(() => {
      expect(mockDeleteReview).toHaveBeenCalledWith('prod-1');
      expect(onReviewsChanged).toHaveBeenCalled();
    });
  });
});

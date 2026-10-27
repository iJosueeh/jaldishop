import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ReviewsView } from './ReviewsView';
import { useCustomerReviews } from '../hooks/useCustomerReviews';

vi.mock('../hooks/useCustomerReviews', () => ({
  useCustomerReviews: vi.fn(),
}));

describe('ReviewsView Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders loading skeleton when fetching reviews', () => {
    vi.mocked(useCustomerReviews).mockReturnValue({
      reviews: [],
      isLoading: true,
      isError: false,
      error: null,
      refetch: vi.fn(),
      createReview: vi.fn(),
      isCreating: false,
      updateReview: vi.fn(),
      isUpdating: false,
    });

    render(<ReviewsView />);
    expect(screen.getByTestId('reviews-loading')).toBeInTheDocument();
  });

  it('renders empty state when customer has no reviews', () => {
    vi.mocked(useCustomerReviews).mockReturnValue({
      reviews: [],
      isLoading: false,
      isError: false,
      error: null,
      refetch: vi.fn(),
      createReview: vi.fn(),
      isCreating: false,
      updateReview: vi.fn(),
      isUpdating: false,
    });

    render(<ReviewsView />);
    expect(screen.getByTestId('reviews-empty')).toBeInTheDocument();
    expect(screen.getByText('Aún no has compartido reseñas')).toBeInTheDocument();
    expect(screen.getByText('Ver mis pedidos completados')).toBeInTheDocument();
  });

  it('renders list of customer reviews with store name and rating', () => {
    vi.mocked(useCustomerReviews).mockReturnValue({
      reviews: [
        {
          id: 'rev-1',
          productId: 'prod-1',
          productName: 'Mueble de Roble Macizo',
          productSlug: 'mueble-de-roble-macizo',
          storeName: 'Carpintería Don Luis',
          storeSlug: 'carpinteria-don-luis',
          imageUrl: null,
          rating: 5,
          comment: 'Impecable acabado y entrega puntual.',
          status: 'PUBLISHED',
          createdAt: '2026-10-25T12:00:00Z',
          updatedAt: '2026-10-25T12:00:00Z',
        },
      ],
      isLoading: false,
      isError: false,
      error: null,
      refetch: vi.fn(),
      createReview: vi.fn(),
      isCreating: false,
      updateReview: vi.fn(),
      isUpdating: false,
    });

    render(<ReviewsView />);
    expect(screen.getByTestId('reviews-list')).toBeInTheDocument();
    expect(screen.getByText('Mueble de Roble Macizo')).toBeInTheDocument();
    expect(screen.getByText('Carpintería Don Luis')).toBeInTheDocument();
    expect(screen.getByText('Impecable acabado y entrega puntual.')).toBeInTheDocument();
    expect(screen.getByText('Compra verificada')).toBeInTheDocument();
  });
});

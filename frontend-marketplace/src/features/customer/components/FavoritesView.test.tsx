import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { FavoritesView } from './FavoritesView';
import { useCustomerFavorites } from '../hooks/useCustomerFavorites';

vi.mock('../hooks/useCustomerFavorites', () => ({
  useCustomerFavorites: vi.fn(),
}));

describe('FavoritesView Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders empty state when customer has no favorites with store exploration CTA', () => {
    vi.mocked(useCustomerFavorites).mockReturnValue({
      favorites: [],
      isLoading: false,
      isFetching: false,
      error: null,
      removeFavorite: vi.fn(),
      isRemoving: false,
      reload: vi.fn(),
    });

    render(<FavoritesView />);
    expect(screen.getByRole('heading', { level: 1, name: /Mis Favoritos/i })).toBeInTheDocument();
    expect(screen.getByRole('status')).toBeInTheDocument();
    expect(
      screen.getByText('Aún no tienes productos guardados en favoritos')
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Explorar comercios locales/i })).toHaveAttribute(
      'href',
      '/#tiendas-destacadas'
    );
  });

  it('renders favorite product cards with price, store, and availability', () => {
    vi.mocked(useCustomerFavorites).mockReturnValue({
      favorites: [
        {
          productId: 'prod-1',
          productName: 'Jarrón de Cerámica Artesanal',
          productSlug: 'jarron-de-ceramica-artesanal',
          productImageUrl: null,
          storeId: 'store-1',
          storeName: 'Taller Artesano Chulucanas',
          storeSlug: 'taller-artesano-chulucanas',
          minPrice: 45.0,
          currency: 'PEN',
          isAvailable: true,
          addedAt: '2026-10-25T12:00:00Z',
        },
      ],
      isLoading: false,
      isFetching: false,
      error: null,
      removeFavorite: vi.fn(),
      isRemoving: false,
      reload: vi.fn(),
    });

    render(<FavoritesView />);
    expect(screen.getByText('Jarrón de Cerámica Artesanal')).toBeInTheDocument();
    expect(screen.getByText('Taller Artesano Chulucanas')).toBeInTheDocument();
    expect(screen.getByText('S/ 45.00')).toBeInTheDocument();
    expect(screen.getByText('Disponible')).toBeInTheDocument();
  });
});

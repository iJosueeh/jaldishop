import { FavoriteProductSummary, FavoriteStatusResponse } from '../types/customer.types';

export const customerFavoriteService = {
  async getFavoriteProducts(): Promise<FavoriteProductSummary[]> {
    const res = await fetch('/api/customer/favorites', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      cache: 'no-store',
    });

    if (!res.ok) {
      const error = await res.json().catch(() => ({}));
      throw new Error(error.message || 'No fue posible cargar tus favoritos.');
    }

    return res.json();
  },

  async addFavorite(productId: string): Promise<FavoriteStatusResponse> {
    const res = await fetch(`/api/customer/favorites/${encodeURIComponent(productId)}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!res.ok) {
      const error = await res.json().catch(() => ({}));
      throw new Error(error.message || 'No se pudo agregar a favoritos.');
    }

    return res.json();
  },

  async removeFavorite(productId: string): Promise<FavoriteStatusResponse> {
    const res = await fetch(`/api/customer/favorites/${encodeURIComponent(productId)}`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!res.ok) {
      const error = await res.json().catch(() => ({}));
      throw new Error(error.message || 'No se pudo eliminar de favoritos.');
    }

    return res.json();
  },

  async checkFavoriteStatus(productId: string): Promise<FavoriteStatusResponse> {
    const res = await fetch(`/api/customer/favorites/${encodeURIComponent(productId)}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      cache: 'no-store',
    });

    if (!res.ok) {
      const error = await res.json().catch(() => ({}));
      throw new Error(error.message || 'No se pudo verificar favorito.');
    }

    return res.json();
  },
};

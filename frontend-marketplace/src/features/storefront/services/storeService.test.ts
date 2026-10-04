import { describe, it, expect, vi, beforeEach } from 'vitest';
import { storeService } from './storeService';
import { apiClient, ApiException } from '@/core/api/apiClient';

vi.mock('@/core/api/apiClient', () => ({
  apiClient: vi.fn(),
  ApiException: class ApiException extends Error {
    constructor(public status: number, public statusText: string) {
      super(`HTTP ${status}: ${statusText}`);
      this.name = 'ApiException';
    }
  },
}));

describe('storeService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('getStoreBySlug', () => {
    it('returns store data when found in backend', async () => {
      const mockStore = {
        id: 'store-1',
        name: 'Panadería Don Pepe',
        slug: 'panaderia-don-pepe',
        status: 'ACTIVE' as const,
        deliveryEnabled: true,
        pickupEnabled: true,
      };

      vi.mocked(apiClient).mockResolvedValueOnce(mockStore);

      const result = await storeService.getStoreBySlug('panaderia-don-pepe');
      expect(result).toEqual(mockStore);
      expect(apiClient).toHaveBeenCalledWith('/stores/slug/panaderia-don-pepe', expect.any(Object));
    });

    it('returns null when store is not found (404)', async () => {
      vi.mocked(apiClient).mockRejectedValueOnce(new ApiException(404, 'Not Found'));

      const result = await storeService.getStoreBySlug('inexistent-store');
      expect(result).toBeNull();
    });

    it('propagates a network failure so it is not shown as a missing store', async () => {
      vi.mocked(apiClient).mockRejectedValueOnce(new Error('Network error'));

      await expect(storeService.getStoreBySlug('any-store')).rejects.toThrow('Network error');
    });

    it('uses the published contact and shipping fields from the backend', async () => {
      vi.mocked(apiClient).mockResolvedValueOnce({ id: '1', name: 'Toddy', whatsappNumber: '51999999999', contactPhone: '51888888888', deliveryFeeAmount: 7.5 });
      const store = await storeService.getStoreBySlug('toddy');
      expect(store?.phone).toBe('51999999999');
      expect(store?.deliveryFee).toBe(7.5);
    });
  });

  describe('getFeaturedStores', () => {
    it('returns active stores from backend', async () => {
      const mockStores = [
        { id: '1', name: 'Tienda 1', slug: 't-1', status: 'ACTIVE' as const, deliveryEnabled: true, pickupEnabled: true },
        { id: '2', name: 'Tienda 2', slug: 't-2', status: 'ACTIVE' as const, deliveryEnabled: true, pickupEnabled: true },
      ];

      vi.mocked(apiClient).mockResolvedValueOnce(mockStores);

      const result = await storeService.getFeaturedStores(6);
      expect(result).toEqual(mockStores);
    });

    it('returns empty array when backend has no featured stores', async () => {
      vi.mocked(apiClient).mockResolvedValueOnce([]);

      const result = await storeService.getFeaturedStores();
      expect(result).toEqual([]);
    });

    it('returns empty array on backend error to trigger clean @empty state', async () => {
      vi.mocked(apiClient).mockRejectedValueOnce(new Error('Connection refused'));

      const result = await storeService.getFeaturedStores();
      expect(result).toEqual([]);
    });
  });

  describe('searchStores', () => {
    it('searches stores with query parameter', async () => {
      const mockResults = [
        { id: '1', name: 'Panadería San José', slug: 'panaderia-san-jose', status: 'ACTIVE' as const, deliveryEnabled: true, pickupEnabled: true },
      ];

      vi.mocked(apiClient).mockResolvedValueOnce(mockResults);

      const result = await storeService.searchStores('pan', 5);
      expect(result).toEqual(mockResults);
      expect(apiClient).toHaveBeenCalledWith('/stores/search', {
        params: { q: 'pan', limit: 5 },
        timeoutMs: 3000,
      });
    });

    it('returns empty array when search yields no matches', async () => {
      vi.mocked(apiClient).mockResolvedValueOnce([]);

      const result = await storeService.searchStores('nonexistent');
      expect(result).toEqual([]);
    });
  });

  describe('getCategories', () => {
    it('returns categories from backend', async () => {
      const mockCategories = [
        { id: 'cat-1', name: 'Panadería', slug: 'panaderia' },
      ];

      vi.mocked(apiClient).mockResolvedValueOnce(mockCategories);

      const result = await storeService.getCategories();
      expect(result).toEqual(mockCategories);
    });

    it('returns empty array when category fetch fails', async () => {
      vi.mocked(apiClient).mockRejectedValueOnce(new Error('Server error'));

      const result = await storeService.getCategories();
      expect(result).toEqual([]);
    });
  });
});

import { apiClient, ApiException } from '@/core/api/apiClient';
import { PublicStore, StoreCategory } from '../types/storefront.types';

export const storeService = {
  /**
   * Fetches public store information by slug from backend API.
   * Returns null if store does not exist (triggering notFound).
   */
  async getStoreBySlug(slug: string): Promise<PublicStore | null> {
    try {
      const store = await apiClient<PublicStore>(`/stores/slug/${slug}`, {
        timeoutMs: 4000,
      });
      return store;
    } catch (error: unknown) {
      if (error instanceof ApiException && error.status === 404) {
        return null;
      }
      return null;
    }
  },

  /**
   * Retrieves featured/active stores from backend API.
   */
  async getFeaturedStores(limit = 6): Promise<PublicStore[]> {
    try {
      const stores = await apiClient<PublicStore[]>('/stores/featured', {
        params: { limit },
        timeoutMs: 4000,
      });
      return Array.isArray(stores) ? stores : [];
    } catch {
      return [];
    }
  },

  /**
   * Searches active stores in real-time. If query is empty, returns initial active stores.
   */
  async searchStores(query: string = '', limit = 5): Promise<PublicStore[]> {
    const cleanQuery = query.trim();

    try {
      const results = await apiClient<PublicStore[]>('/stores/search', {
        params: { q: cleanQuery, limit },
        timeoutMs: 3000,
      });
      return Array.isArray(results) ? results.slice(0, limit) : [];
    } catch {
      return [];
    }
  },

  /**
   * Fetches all active store categories from backend API.
   */
  async getCategories(): Promise<StoreCategory[]> {
    try {
      const categories = await apiClient<StoreCategory[]>('/store-categories', {
        timeoutMs: 4000,
      });
      return Array.isArray(categories) ? categories : [];
    } catch {
      return [];
    }
  },
};

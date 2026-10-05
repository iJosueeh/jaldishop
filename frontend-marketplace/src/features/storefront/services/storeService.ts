import { apiClient, ApiException } from '@/core/api/apiClient';
import { ProductItem, ProductVariantItem, PublicStore, StoreCategory } from '../types/storefront.types';

interface CatalogCategoryDto {
  id: string; storeId: string; name: string; status: 'ACTIVE' | 'INACTIVE';
}
interface PublicProductDto {
  id: string; storeId: string; categoryId?: string; name: string; description?: string;
  imageUrl?: string; minPrice?: number | null; maxPrice?: number | null;
  variants?: ProductVariantItem[];
}

async function loadStoreProducts(storePath: string): Promise<ProductItem[]> {
  try {
    const [products, categories] = await Promise.all([
      apiClient<PublicProductDto[]>(storePath + '/products', { timeoutMs: 12000, revalidate: 300 }),
      apiClient<CatalogCategoryDto[]>(storePath + '/categories', { timeoutMs: 12000, revalidate: 300 }).catch(() => []),
    ]);
    if (!Array.isArray(products)) return [];
    const catalogCategories = Array.isArray(categories) ? categories : [];
    return products.map((product) => {
      const category = catalogCategories.find((entry) => entry.id === product.categoryId && entry.storeId === product.storeId && entry.status === 'ACTIVE');
      const price = product.minPrice ?? product.variants?.[0]?.priceAmount ?? 0;
      return {
        id: product.id, name: product.name, description: product.description || '',
        price: Number(price), minPrice: product.minPrice != null ? Number(product.minPrice) : undefined,
        maxPrice: product.maxPrice != null ? Number(product.maxPrice) : undefined,
        categoryId: product.categoryId, category: category?.name || '',
        imageUrl: product.imageUrl, imageBg: 'from-amber-700/20 to-stone-800/30',
        iconText: product.name.substring(0, 2).toUpperCase(), variants: product.variants,
      };
    });
  } catch (err) {
    console.error(`[loadStoreProducts] Error consultando productos en ${storePath}:`, err);
    return [];
  }
}

export const storeService = {
  async getStoreBySlug(slug: string): Promise<PublicStore | null> {
    try {
      const store = await apiClient<PublicStore>(`/stores/slug/${slug}`, {
        timeoutMs: 12000,
        revalidate: 300,
      });
      if (!store) return null;

      let category = store.category;
      if (!category && store.categoryIds && store.categoryIds.length > 0) {
        try {
          const categories = await storeService.getCategories();
          const found = categories.find((c) => store.categoryIds?.includes(c.id));
          if (found) {
            category = found.name;
          }
        } catch {
          // ignore lookup error and fallback gracefully
        }
      }

      return {
        ...store,
        category: category || store.category,
        ...((store.whatsappNumber || store.contactPhone) ? { phone: store.whatsappNumber || store.contactPhone } : {}),
        ...(typeof store.deliveryFeeAmount === 'number' ? { deliveryFee: store.deliveryFeeAmount } : {}),
      };
    } catch (error: unknown) {
      if (error instanceof ApiException && error.status === 404) {
        return null;
      }
      throw error;
    }
  },

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

  getStoreProducts(storeId: string): Promise<ProductItem[]> {
    return loadStoreProducts('/stores/' + encodeURIComponent(storeId));
  },

  getStoreProductsBySlug(slug: string): Promise<ProductItem[]> {
    return loadStoreProducts('/stores/slug/' + encodeURIComponent(slug));
  },
};

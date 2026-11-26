import { apiClient, ApiException } from '@/core/api/apiClient';
import {
  MarketplaceProductItem,
  ProductItem,
  ProductReviewsSummary,
  ProductVariantItem,
  PublicStore,
  PublicStoreSettings,
  StoreCategory,
} from '../types/storefront.types';

interface CatalogCategoryDto {
  id: string; storeId: string; name: string; status: 'ACTIVE' | 'INACTIVE';
}
interface PublicProductDto {
  id: string; storeId: string; categoryId?: string; name: string; slug?: string; description?: string;
  imageUrl?: string; images?: string[]; status?: 'ACTIVE' | 'INACTIVE' | 'ARCHIVED'; minPrice?: number | null; maxPrice?: number | null;
  variants?: ProductVariantItem[]; createdAt?: string; updatedAt?: string;
}

export function normalizeProductImages(images?: string[], fallbackImageUrl?: string): string[] {
  const unique = new Set<string>();
  if (Array.isArray(images)) {
    for (const url of images) {
      if (typeof url === 'string' && url.trim()) {
        unique.add(url.trim());
      }
    }
  }
  if (typeof fallbackImageUrl === 'string' && fallbackImageUrl.trim()) {
    unique.add(fallbackImageUrl.trim());
  }
  return Array.from(unique);
}

export function toProductItem(product: PublicProductDto, categories: CatalogCategoryDto[]): ProductItem {
  const category = categories.find((entry) => entry.id === product.categoryId && entry.storeId === product.storeId && entry.status === 'ACTIVE');
  const activeVariants = product.variants?.filter((variant) => variant.status === 'ACTIVE');
  const price = product.minPrice ?? activeVariants?.[0]?.priceAmount ?? product.variants?.[0]?.priceAmount ?? 0;
  const priceCurrency = activeVariants?.[0]?.priceCurrency ?? product.variants?.[0]?.priceCurrency ?? 'PEN';
  const normalizedImages = normalizeProductImages(product.images, product.imageUrl);
  const primaryImageUrl =
    (typeof product.imageUrl === 'string' && product.imageUrl.trim()) || normalizedImages[0] || undefined;

  return {
    id: product.id, slug: product.slug, name: product.name, description: product.description || '',
    price: Number(price), priceCurrency,
    minPrice: product.minPrice != null ? Number(product.minPrice) : undefined,
    maxPrice: product.maxPrice != null ? Number(product.maxPrice) : undefined,
    categoryId: product.categoryId, category: category?.name || '',
    status: product.status,
    imageUrl: primaryImageUrl,
    images: normalizedImages,
    imageBg: 'from-amber-700/20 to-stone-800/30',
    iconText: product.name.substring(0, 2).toUpperCase(), variants: product.variants,
    createdAt: product.createdAt, updatedAt: product.updatedAt,
  };
}

async function loadStoreProducts(storePath: string): Promise<ProductItem[]> {
  try {
    const [products, categories] = await Promise.all([
      apiClient<PublicProductDto[]>(storePath + '/products', { timeoutMs: 12000, revalidate: 300 }),
      apiClient<CatalogCategoryDto[]>(storePath + '/categories', { timeoutMs: 12000, revalidate: 300 }).catch(() => []),
    ]);
    if (!Array.isArray(products)) return [];
    const catalogCategories = Array.isArray(categories) ? categories : [];
    return products.map((product) => toProductItem(product, catalogCategories));
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
        revalidate: 60,
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

  async getStoreBySlugClient(slug: string): Promise<PublicStore | null> {
    try {
      const store = await apiClient<PublicStore>(`/api/stores/${encodeURIComponent(slug)}`, {
        baseUrl: '',
        timeoutMs: 12000,
      });
      if (!store) return null;
      return {
        ...store,
        ...((store.whatsappNumber || store.contactPhone) ? { phone: store.whatsappNumber || store.contactPhone } : {}),
        ...(typeof store.deliveryFeeAmount === 'number' ? { deliveryFee: store.deliveryFeeAmount } : {}),
      };
    } catch (error: unknown) {
      if (error instanceof ApiException && error.status === 404) return null;
      throw error;
    }
  },

  async getPublicStoreSettings(storeId: string): Promise<PublicStoreSettings | null> {
    try {
      return await apiClient<PublicStoreSettings>(`/stores/${encodeURIComponent(storeId)}/public-settings`, {
        timeoutMs: 5000,
        revalidate: 30,
      });
    } catch {
      return null;
    }
  },

  async getPublicStoreSettingsClient(storeId: string): Promise<PublicStoreSettings | null> {
    try {
      return await apiClient<PublicStoreSettings>(`/api/stores/settings/${encodeURIComponent(storeId)}`, {
        baseUrl: '',
        timeoutMs: 5000,
      });
    } catch {
      return null;
    }
  },

  async getStoreProductBySlug(storeSlug: string, productSlug: string): Promise<ProductItem | null> {
    try {
      const [product, categories] = await Promise.all([
        apiClient<PublicProductDto>(`/stores/slug/${encodeURIComponent(storeSlug)}/products/${encodeURIComponent(productSlug)}`, { timeoutMs: 12000, revalidate: 300 }),
        apiClient<CatalogCategoryDto[]>(`/stores/slug/${encodeURIComponent(storeSlug)}/categories`, { timeoutMs: 12000, revalidate: 300 }).catch(() => []),
      ]);
      return toProductItem(product, Array.isArray(categories) ? categories : []);
    } catch (error) {
      if (error instanceof ApiException && error.status === 404) return null;
      throw error;
    }
  },

  async getPublicProductReviews(productId: string): Promise<ProductReviewsSummary | null> {
    try {
      return await apiClient<ProductReviewsSummary>(`/products/${encodeURIComponent(productId)}/reviews`, {
        timeoutMs: 5000,
        revalidate: 60,
      });
    } catch {
      return null;
    }
  },

  async getStoreProductBySlugLive(storeSlug: string, productSlug: string): Promise<ProductItem | null> {
    const res = await fetch(
      `/api/stores/${encodeURIComponent(storeSlug)}/products/${encodeURIComponent(productSlug)}`,
      { method: 'GET', headers: { Accept: 'application/json' }, cache: 'no-store' }
    );
    if (res.status === 404) return null;
    if (!res.ok) throw new Error('No se pudo actualizar la disponibilidad del producto.');
    return res.json();
  },

  async getPublicProductReviewsLive(productId: string): Promise<ProductReviewsSummary | null> {
    const res = await fetch(`/api/products/${encodeURIComponent(productId)}/reviews`, {
      method: 'GET',
      headers: { Accept: 'application/json' },
      cache: 'no-store',
    });
    if (!res.ok) return null;
    return res.json();
  },

  selectRelatedProducts(
    allProducts: ProductItem[],
    currentProduct: ProductItem,
    limit = 8
  ): ProductItem[] {
    return allProducts
      .filter((item) => item.id !== currentProduct.id)
      .sort((a, b) => {
        const aSameCat =
          (currentProduct.categoryId && a.categoryId === currentProduct.categoryId) ||
          (currentProduct.category && a.category === currentProduct.category)
            ? 1
            : 0;
        const bSameCat =
          (currentProduct.categoryId && b.categoryId === currentProduct.categoryId) ||
          (currentProduct.category && b.category === currentProduct.category)
            ? 1
            : 0;
        return bSameCat - aSameCat;
      })
      .slice(0, limit);
  },

  async getAllPublicStores(query = '', limit = 50): Promise<PublicStore[]> {
    try {
      const stores = await apiClient<PublicStore[]>('/stores/search', {
        params: { q: query.trim(), limit },
        timeoutMs: 6000,
        revalidate: 60,
      });
      return Array.isArray(stores) ? stores : [];
    } catch {
      return [];
    }
  },

  async getPaginatedPublicStores(params: {
    q?: string;
    page?: number;
    pageSize?: number;
  } = {}): Promise<{
    items: PublicStore[];
    total: number;
    page: number;
    pageSize: number;
    totalPages: number;
  }> {
    const query = new URLSearchParams();
    if (params.q?.trim()) query.set('q', params.q.trim());
    query.set('page', String(params.page ?? 1));
    query.set('pageSize', String(params.pageSize ?? 9));

    const res = await fetch(`/api/stores?${query.toString()}`, {
      method: 'GET',
      headers: { Accept: 'application/json' },
    });

    if (!res.ok) {
      throw new Error('No pudimos cargar las tiendas.');
    }

    return res.json();
  },

  async getMarketplaceProducts(
    limitPerStore = 6,
    maxStores = 8
  ): Promise<MarketplaceProductItem[]> {
    const stores = await storeService.getFeaturedStores(maxStores);
    if (stores.length === 0) return [];

    const productsPerStore = await Promise.all(
      stores.map(async (store) => {
        const products = await storeService.getStoreProductsBySlug(store.slug);
        return products.slice(0, limitPerStore).map((product) => ({
          id: product.id,
          name: product.name,
          slug: product.slug,
          description: product.description,
          price: product.price,
          priceCurrency: product.priceCurrency || 'PEN',
          imageUrl: product.imageUrl,
          images: product.images,
          category: product.category || store.category || 'Catálogo local',
          storeId: store.id,
          storeName: store.name,
          storeSlug: store.slug,
          deliveryEnabled: store.deliveryEnabled,
          pickupEnabled: store.pickupEnabled,
        }));
      })
    );

    return productsPerStore.flat();
  },
};

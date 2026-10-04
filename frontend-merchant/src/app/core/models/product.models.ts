export interface ProductCategory {
  id: string;
  storeId?: string;
  name: string;
  description?: string;
  productCount?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface ProductImage {
  id?: string;
  productId?: string;
  imageUrl: string;
  position: number;
  isPrimary: boolean;
  createdAt?: string;
  file?: File;
}

export interface ProductVariantAttribute {
  name: string;
  value: string;
}

export interface ProductVariant {
  id: string;
  productId: string;
  presentationName: string;
  sku: string;
  priceAmount: number;
  priceCurrency: string;
  tracksInventory: boolean;
  status: 'ACTIVE' | 'INACTIVE' | 'ARCHIVED';
  attributes?: ProductVariantAttribute[];
  createdAt?: string;
  updatedAt?: string;
}

export interface Product {
  id: string;
  storeId: string;
  categoryId?: string;
  categoryName?: string;
  name: string;
  slug: string;
  description?: string;
  imageUrl?: string;
  images?: ProductImage[];
  status: 'ACTIVE' | 'INACTIVE' | 'ARCHIVED';
  createdAt?: string;
  updatedAt?: string;
  variants?: ProductVariant[];
  // Campos de presentación para la UI del merchant
  minPrice?: number;
  maxPrice?: number;
  stockCount?: number;
  preparedCount?: number;
  quotaToday?: { current: number; total: number };
  leadTimeText?: string;
  badgeText?: string;
  badgeType?: 'bestseller' | 'encargo' | 'paused' | 'popular' | 'classic' | 'artesanal';
  channelText?: string;
  hasStockIssue?: boolean;
  stockIssueMessage?: string;
}

export interface CreateCategoryRequest {
  name: string;
  description?: string;
}

export interface CreateProductRequest {
  categoryId: string;
  name: string;
  slug?: string;
  description?: string;
  imageUrl?: string;
  images?: ProductImage[];
}

export interface CreateProductVariantRequest {
  presentationName: string;
  sku?: string;
  priceAmount: number;
  priceCurrency: string;
  tracksInventory: boolean;
  attributes?: ProductVariantAttribute[];
}

export interface UpdateProductVariantRequest {
  presentationName: string;
  sku?: string;
  priceAmount: number;
  priceCurrency: string;
  tracksInventory: boolean;
  status?: 'ACTIVE' | 'INACTIVE' | 'ARCHIVED';
  attributes?: ProductVariantAttribute[];
}

export interface CreateProductVariantItem {
  presentationName: string;
  sku?: string;
  priceAmount: number;
  priceCurrency?: string;
  tracksInventory: boolean;
  attributes?: ProductVariantAttribute[];
}

export interface CreateProductWithVariantPayload {
  categoryId?: string;
  newCategoryName?: string;
  name: string;
  slug?: string;
  description?: string;
  imageUrl?: string;
  images?: ProductImage[];
  presentationName?: string;
  sku?: string;
  priceAmount?: number;
  priceCurrency?: string;
  tracksInventory?: boolean;
  attributes?: ProductVariantAttribute[];
  variants?: CreateProductVariantItem[];
}

export interface UpdateProductRequest {
  categoryId?: string;
  name?: string;
  slug?: string;
  description?: string;
  imageUrl?: string;
  images?: ProductImage[];
  status?: 'ACTIVE' | 'INACTIVE' | 'ARCHIVED';
}

export type ProductViewMode = 'grid' | 'list';

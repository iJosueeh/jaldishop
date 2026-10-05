export interface PublicStore {
  id: string;
  name: string;
  slug: string;
  description?: string;
  tagline?: string;
  bannerUrl?: string;
  logoUrl?: string;
  phone?: string;
  contactPhone?: string;
  whatsappNumber?: string;
  deliveryFeeAmount?: number;
  address?: string;
  addressReference?: string;
  latitude?: number;
  longitude?: number;
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
  deliveryEnabled: boolean;
  pickupEnabled: boolean;
  deliveryFee?: number;
  deliveryFeeCurrency?: string;
  minOrderAmount?: number;
  category?: string;
  categoryIds?: string[];
  rating?: number;
  reviewsCount?: number;
  preparationTimeMinutes?: number;
  openingHours?: string;
  badge?: string;
  badgeVariant?: 'jade' | 'terracotta' | 'amber';
  iconEmoji?: string;
  coverGradient?: string;
  logoBg?: string;
  keywords?: string[];
}

export interface StoreCategory {
  id: string;
  name: string;
  slug: string;
  description?: string;
  icon?: string;
  itemCount?: number;
}

export interface ProductVariantItem {
  id: string;
  presentationName: string;
  sku?: string;
  priceAmount: number;
  priceCurrency: string;
  tracksInventory: boolean;
  status: 'ACTIVE' | 'INACTIVE' | 'ARCHIVED';
}

export interface ProductItem {
  categoryId?: string;
  priceCurrency?: string;
  variantId?: string;
  id: string;
  name: string;
  description: string;
  price: number;
  category: string;
  badge?: string;
  badgeVariant?: 'jade' | 'terracotta' | 'amber';
  imageBg?: string;
  iconText?: string;
  imageUrl?: string;
  prepTimeMinutes?: number;
  minPrice?: number;
  maxPrice?: number;
  variants?: ProductVariantItem[];
}

export interface CartItem {
  available?: boolean;
  product: ProductItem;
  quantity: number;
  notes?: string;
}

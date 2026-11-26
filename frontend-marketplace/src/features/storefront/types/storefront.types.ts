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
  instagramUrl?: string;
  facebookUrl?: string;
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
  canIssueInvoices?: boolean;
  taxRate?: number;
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

export interface PublicStoreSettings {
  cancellationPolicy: string;
  refundPolicy: string;
  requireCustomerDni: boolean;
  allowOrderNotes: boolean;
  onlinePaymentsEnabled: boolean;
  cashOnDeliveryEnabled: boolean;
  acceptsPosOnDelivery: boolean;
  minDeliveryAmount: number | null;
  estimatedPrepTimeMinutes: number;
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
  productId?: string;
  presentationName: string;
  sku?: string;
  priceAmount: number;
  priceCurrency: string;
  tracksInventory: boolean;
  availableQuantity?: number | null;
  status: 'ACTIVE' | 'INACTIVE' | 'ARCHIVED';
  attributes?: { name: string; value: string }[];
}

export interface ProductItem {
  slug?: string;
  categoryId?: string;
  priceCurrency?: string;
  variantId?: string;
  id: string;
  name: string;
  description: string;
  price: number;
  category: string;
  status?: 'ACTIVE' | 'INACTIVE' | 'ARCHIVED';
  badge?: string;
  badgeVariant?: 'jade' | 'terracotta' | 'amber';
  imageBg?: string;
  iconText?: string;
  imageUrl?: string;
  images?: string[];
  prepTimeMinutes?: number;
  minPrice?: number;
  maxPrice?: number;
  variants?: ProductVariantItem[];
  createdAt?: string;
  updatedAt?: string;
}

export interface CartItem {
  available?: boolean;
  product: ProductItem;
  quantity: number;
  notes?: string;
}

export interface PublicReviewItem {
  id: string;
  customerName: string;
  rating: number;
  comment: string | null;
  createdAt: string;
}

export interface ProductReviewsSummary {
  productId: string;
  averageRating: number;
  totalReviews: number;
  reviews: PublicReviewItem[];
}

export interface MarketplaceProductItem {
  id: string;
  name: string;
  slug?: string;
  description: string;
  price: number;
  priceCurrency?: string;
  imageUrl?: string;
  images?: string[];
  category: string;
  storeId: string;
  storeName: string;
  storeSlug: string;
  deliveryEnabled: boolean;
  pickupEnabled: boolean;
}

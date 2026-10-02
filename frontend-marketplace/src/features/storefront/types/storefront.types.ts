export interface PublicStore {
  id: string;
  name: string;
  slug: string;
  description?: string;
  tagline?: string;
  bannerUrl?: string;
  logoUrl?: string;
  phone?: string;
  address?: string;
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
  deliveryEnabled: boolean;
  pickupEnabled: boolean;
  deliveryFee?: number;
  minOrderAmount?: number;
  category?: string;
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

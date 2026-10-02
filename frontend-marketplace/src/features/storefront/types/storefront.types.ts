export interface PublicStore {
  id: string;
  name: string;
  slug: string;
  description?: string;
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
}

export interface StoreCategory {
  id: string;
  name: string;
  slug: string;
  itemCount?: number;
}

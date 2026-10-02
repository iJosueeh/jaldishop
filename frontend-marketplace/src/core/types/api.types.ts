/**
 * JaldiShop API Types and Data Transfer Contracts
 */

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  details?: Record<string, string>;
}

export interface PublicStoreDto {
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
}

export interface ApiResponse<T> {
  data: T;
  message?: string;
  success: boolean;
}

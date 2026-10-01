export interface StoreResponse {
    id: string;
    merchantUserId: string;
    name: string;
    slug: string;
    description?: string;
    contactPhone?: string;
    address?: string;
    addressReference?: string;
    latitude?: number;
    longitude?: number;
    pickupEnabled: boolean;
    deliveryEnabled: boolean;
    deliveryFeeAmount?: number;
    deliveryFeeCurrency?: string;
    taxRate?: number;
    logoUrl?: string;
    bannerUrl?: string;
    instagramUrl?: string;
    facebookUrl?: string;
    whatsappNumber?: string;
    status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'CLOSED';
    createdAt: string;
    updatedAt: string; 
}

export interface StoreCategory {
    id: string;
    name: string;
    slug: string;
    description?: string;
    status: 'ACTIVE' | 'INACTIVE';
}

export interface CreateStoreRequest {
    name: string;
    slug?: string;
    description?: string;
    contactPhone?: string;
    address?: string;
    addressReference?: string;
    latitude?: number;
    longitude?: number;
    pickupEnabled: boolean;
    deliveryEnabled: boolean;
    deliveryFeeAmount?: number;
    deliveryFeeCurrency?: string;
    taxRate?: number;
    logoUrl?: string;
    bannerUrl?: string;
    instagramUrl?: string;
    facebookUrl?: string;
    whatsappNumber?: string;
}

export type UpdateStoreRequest = Omit<CreateStoreRequest, 'slug'>;
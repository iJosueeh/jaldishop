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
    taxApplies: boolean;
    taxRate?: number;
    status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'CLOSED';
    createdAt: string;
    updatedAt: string; 
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
    taxApplies: boolean;
    taxRate?: number;
}

export type UpdateStoreRequest = Omit<CreateStoreRequest, 'slug'>;
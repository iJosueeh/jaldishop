export type TaxIdType = 'DNI' | 'RUC_10' | 'RUC_20';
export type TaxRegime = 'NONE' | 'RUS' | 'RER' | 'MYPE' | 'GENERAL';

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
    categoryIds?: string[];
    status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'CLOSED';
    legalBusinessName?: string;
    taxIdType?: TaxIdType;
    taxIdNumber?: string;
    taxRegime?: TaxRegime;
    canIssueInvoices?: boolean;
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
    categoryIds?: string[];
    legalBusinessName?: string;
    taxIdType?: TaxIdType;
    taxIdNumber?: string;
    taxRegime?: TaxRegime;
    canIssueInvoices?: boolean;
}

export type UpdateStoreRequest = Omit<CreateStoreRequest, 'slug'>;

export interface UpdateStoreTaxProfileRequest {
    legalBusinessName?: string;
    taxIdType: TaxIdType;
    taxIdNumber: string;
    taxRegime: TaxRegime;
    canIssueInvoices: boolean;
}

export interface CloseStoreRequest {
    reason?: string;
}

export interface CloseStoreResponse {
    storeId: string;
    action: 'DELETED' | 'DEACTIVATED';
    status?: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED' | 'CLOSED';
    message: string;
}
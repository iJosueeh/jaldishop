export interface LoginRequest {
    email: string;
    password: string;
}

export interface RegisterMerchantRequest {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
    phone?: string;
    storeName: string;
    businessType?: string;
    storeContactPhone?: string;
    address?: string;
    pickupEnabled: boolean;
    deliveryEnabled: boolean;
}

export interface AuthResult {
    token: string;
    userId: string;
    email: string;
    fullName: string;
    roles: string[];
}

export interface UserResponse {
    id: string;
    email: string;
    firstName: string;
    lastName: string;
    phone: string;
    status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
    createdAt?: string;
}

export interface ApiError {
    code: string;
    message: string;
    errors?: Record<string, string>
}
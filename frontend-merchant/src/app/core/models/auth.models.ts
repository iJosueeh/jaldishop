export interface LoginRequest {
    email: string;
    password: string;
}

export interface RegisterMerchantRequest {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
    phone: string;
}

export interface AuthResult {
    token: string;
    expiresAt: number;
    user: UserResponse;
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
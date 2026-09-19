export interface UserProfile {
    id: string;
    email: string;
    firstName: string;
    lastName: string;
    phone: string | null;
    status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
    roles: string[];
    createdAt: string;
}

export interface UpdateUserProfileRequest {
    firstName: string;
    lastName: string;
    phone?: string | null;
}
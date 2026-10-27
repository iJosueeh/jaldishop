export interface LoginCredentials {
  email: string;
  password: string;
}

export interface RegisterCustomerData {
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  password: string;
}

export interface AuthSessionResponse {
  authenticated: boolean;
  fullName?: string;
  message?: string;
}

export interface RegisterCustomerResponse {
  authenticated: boolean;
  autoLogin: boolean;
  fullName?: string;
  message?: string;
  errors?: Record<string, string>;
}

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
  requiresEmailVerification?: boolean;
  email?: string;
  fullName?: string;
  message?: string;
  errors?: Record<string, string>;
}

export interface VerifyEmailRequest {
  token: string;
}

export interface ResendVerificationRequest {
  email: string;
}

export interface ResendVerificationResponse {
  message: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ForgotPasswordResponse {
  message: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}


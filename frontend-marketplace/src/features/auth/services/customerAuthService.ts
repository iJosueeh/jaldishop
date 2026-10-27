import { apiClient } from '@/core/api/apiClient';
import {
  AuthSessionResponse,
  LoginCredentials,
  RegisterCustomerData,
  RegisterCustomerResponse,
} from '../types/auth.types';

export const customerAuthService = {
  /**
   * Retrieves current customer authentication status from BFF cookie.
   */
  getSession: () =>
    apiClient<{ authenticated: boolean; fullName?: string }>('/api/customer/session', {
      baseUrl: '',
      cache: 'no-store',
    }),

  /**
   * Logs in customer via BFF route handler, setting HttpOnly session cookie.
   */
  login: (credentials: LoginCredentials) =>
    apiClient<AuthSessionResponse>('/api/customer/session', {
      baseUrl: '',
      method: 'POST',
      body: JSON.stringify(credentials),
    }),

  /**
   * Registers customer via BFF route handler and performs server-side auto-login.
   */
  register: (data: RegisterCustomerData) =>
    apiClient<RegisterCustomerResponse>('/api/customer/register', {
      baseUrl: '',
      method: 'POST',
      body: JSON.stringify(data),
    }),

  /**
   * Clears customer session cookie in BFF.
   */
  logout: () =>
    apiClient<{ authenticated: boolean }>('/api/customer/session', {
      baseUrl: '',
      method: 'DELETE',
    }),
};

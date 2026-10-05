import { apiClient } from '@/core/api/apiClient';

const endpoint = '/api/customer/session';
export const customerSessionService = {
  get: () => apiClient<{ authenticated: boolean }>(endpoint, { baseUrl: '', cache: 'no-store' }),
  login: (email: string, password: string) => apiClient<{ authenticated: boolean }>(endpoint, { baseUrl: '', method: 'POST', body: JSON.stringify({ email, password }) }),
  logout: () => apiClient(endpoint, { baseUrl: '', method: 'DELETE' }),
};

import { apiClient } from '@/core/api/apiClient';

export interface CartResponse {
  id: string | null;
  storeId: string;
  items: {
    variantId: string; productId: string; productName: string; presentationName: string;
    imageUrl?: string; quantity: number; unitPriceAmount: number; unitPriceCurrency: string;
    subtotalAmount: number; available: boolean;
  }[];
  totalItems: number;
  totalAmount: number;
  currency: string;
}

const endpoint = '/api/customer/cart';
const options = { baseUrl: '', cache: 'no-store' as const };
export const cartService = {
  get: (storeId: string) => apiClient<CartResponse>(endpoint, { ...options, params: { storeId } }),
  add: (storeId: string, variantId: string, quantity: number) => apiClient<CartResponse>(`${endpoint}/items`, { ...options, method: 'POST', body: JSON.stringify({ storeId, variantId, quantity }) }),
  update: (storeId: string, variantId: string, quantity: number) => apiClient<CartResponse>(`${endpoint}/items/${variantId}`, { ...options, method: 'PUT', body: JSON.stringify({ storeId, quantity }) }),
  remove: (storeId: string, variantId: string) => apiClient<CartResponse>(`${endpoint}/items/${variantId}`, { ...options, method: 'DELETE', params: { storeId } }),
  clear: (storeId: string) => apiClient<CartResponse>(endpoint, { ...options, method: 'DELETE', params: { storeId } }),
};

import { beforeEach, describe, expect, it, vi } from 'vitest';
import { apiClient } from '@/core/api/apiClient';
import { cartService } from './cartService';

vi.mock('@/core/api/apiClient', () => ({ apiClient: vi.fn() }));
beforeEach(() => vi.clearAllMocks());

describe('Cart API contract', () => {
  it('queries by store and posts variant IDs without trusting client prices', async () => {
    await cartService.get('store-id');
    expect(apiClient).toHaveBeenLastCalledWith('/api/customer/cart', expect.objectContaining({ baseUrl: '', params: { storeId: 'store-id' }, cache: 'no-store' }));
    await cartService.add('store-id', 'variant-id', 2);
    expect(apiClient).toHaveBeenLastCalledWith('/api/customer/cart/items', expect.objectContaining({ method: 'POST', body: JSON.stringify({ storeId: 'store-id', variantId: 'variant-id', quantity: 2 }) }));
  });
  it('uses the backend update, removal and clear contracts', async () => {
    await cartService.update('store-id', 'variant-id', 3);
    expect(apiClient).toHaveBeenLastCalledWith('/api/customer/cart/items/variant-id', expect.objectContaining({ method: 'PUT', body: JSON.stringify({ storeId: 'store-id', quantity: 3 }) }));
    await cartService.remove('store-id', 'variant-id');
    expect(apiClient).toHaveBeenLastCalledWith('/api/customer/cart/items/variant-id', expect.objectContaining({ method: 'DELETE', params: { storeId: 'store-id' } }));
    await cartService.clear('store-id');
    expect(apiClient).toHaveBeenLastCalledWith('/api/customer/cart', expect.objectContaining({ method: 'DELETE', params: { storeId: 'store-id' } }));
  });
  it('propagates errors instead of returning a fabricated empty cart', async () => {
    vi.mocked(apiClient).mockRejectedValueOnce(new Error('Servidor no disponible'));
    await expect(cartService.get('store-id')).rejects.toThrow('Servidor no disponible');
  });
});

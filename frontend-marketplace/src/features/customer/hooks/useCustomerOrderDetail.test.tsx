import { renderHook, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import React from 'react';
import { useCustomerOrderDetail } from './useCustomerOrderDetail';
import { customerOrderService } from '../services/customerOrderService';
import { CustomerOrderDetail } from '../types/customer.types';
import { CUSTOMER_ORDERS_QUERY_KEY } from './useCustomerOrders';

vi.mock('../services/customerOrderService', () => ({
  customerOrderService: {
    getOrderDetail: vi.fn(),
    cancelOrder: vi.fn(),
  },
}));

const mockOrder: CustomerOrderDetail = {
  id: 'uuid-1',
  orderNumber: 'ORD-100',
  store: { id: 's1', name: 'Tienda', slug: 'tienda' },
  status: 'CONFIRMED',
  deliveryMode: 'DELIVERY',
  serviceDate: '2026-11-14',
  serviceStartTime: '10:00:00',
  serviceEndTime: '11:00:00',
  customerName: 'Ana',
  customerPhone: '999',
  customerEmail: 'ana@example.com',
  currency: 'PEN',
  productsSubtotalAmount: 50,
  discountAmount: 0,
  deliveryFeeAmount: 5,
  includedTaxAmount: 7,
  totalAmount: 55,
  confirmedAt: '2026-11-13T10:00:00Z',
  updatedAt: '2026-11-13T10:00:00Z',
  items: [],
  statusHistory: [],
};

describe('useCustomerOrderDetail Hook', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    });
  });

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  it('fetches and returns order detail', async () => {
    vi.mocked(customerOrderService.getOrderDetail).mockResolvedValue(mockOrder);

    const { result } = renderHook(() => useCustomerOrderDetail('ORD-100'), { wrapper });

    await waitFor(() => {
      expect(result.current.order).toEqual(mockOrder);
      expect(result.current.isLoading).toBe(false);
    });
  });

  it('executes cancelOrder mutation, updates cache and invalidates orders list on verified CANCELLED', async () => {
    const cancelledOrder: CustomerOrderDetail = {
      ...mockOrder,
      status: 'CANCELLED',
    };

    vi.mocked(customerOrderService.getOrderDetail)
      .mockResolvedValueOnce(mockOrder)
      .mockResolvedValue(cancelledOrder);
    vi.mocked(customerOrderService.cancelOrder).mockResolvedValue(cancelledOrder);

    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

    const { result } = renderHook(() => useCustomerOrderDetail('ORD-100'), { wrapper });

    await waitFor(() => {
      expect(result.current.order).toEqual(mockOrder);
    });

    await result.current.cancelOrder('Ya no necesito los productos');

    expect(customerOrderService.cancelOrder).toHaveBeenCalledWith(
      'ORD-100',
      'Ya no necesito los productos'
    );

    // Verify cache updated
    expect(queryClient.getQueryData(['customer-order', 'ORD-100'])).toEqual(cancelledOrder);

    // Verify invalidation of customer-orders and customer-order
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: CUSTOMER_ORDERS_QUERY_KEY });
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['customer-order', 'ORD-100'] });
  });

  it('does NOT update cache if mutation response is inconsistent or not CANCELLED', async () => {
    vi.mocked(customerOrderService.getOrderDetail).mockResolvedValue(mockOrder);
    // Simulated inconsistent DTO bypassing service check
    const inconsistentOrder: CustomerOrderDetail = {
      ...mockOrder,
      orderNumber: 'ORD-DIFFERENT',
      status: 'CONFIRMED',
    };
    vi.mocked(customerOrderService.cancelOrder).mockResolvedValue(inconsistentOrder);

    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

    const { result } = renderHook(() => useCustomerOrderDetail('ORD-100'), { wrapper });

    await waitFor(() => {
      expect(result.current.order).toEqual(mockOrder);
    });

    await result.current.cancelOrder();

    // Cache should remain original mockOrder, not inconsistentOrder
    expect(queryClient.getQueryData(['customer-order', 'ORD-100'])).toEqual(mockOrder);
    expect(invalidateSpy).not.toHaveBeenCalledWith({ queryKey: CUSTOMER_ORDERS_QUERY_KEY });
  });

  it('handles cancelOrder mutation failure and captures cancelError', async () => {
    vi.mocked(customerOrderService.getOrderDetail).mockResolvedValue(mockOrder);
    vi.mocked(customerOrderService.cancelOrder).mockRejectedValue(
      new Error('El pedido no se puede cancelar en estado IN_PREPARATION.')
    );

    const { result } = renderHook(() => useCustomerOrderDetail('ORD-100'), { wrapper });

    await waitFor(() => {
      expect(result.current.order).toEqual(mockOrder);
    });

    await expect(result.current.cancelOrder()).rejects.toThrow(
      'El pedido no se puede cancelar en estado IN_PREPARATION.'
    );

    await waitFor(() => {
      expect(result.current.cancelError).toBe(
        'El pedido no se puede cancelar en estado IN_PREPARATION.'
      );
    });

    // Reset error
    result.current.resetCancelError();
    await waitFor(() => {
      expect(result.current.cancelError).toBeNull();
    });
  });

  it('handles timeout uncertain error and preserves error message for UI', async () => {
    vi.mocked(customerOrderService.getOrderDetail).mockResolvedValue(mockOrder);
    vi.mocked(customerOrderService.cancelOrder).mockRejectedValue(
      new Error(
        'No pudimos confirmar la cancelación debido a un problema de conexión. El pedido continúa en estado confirmado. Por favor, actualiza la vista antes de volver a intentarlo.'
      )
    );

    const { result } = renderHook(() => useCustomerOrderDetail('ORD-100'), { wrapper });

    await waitFor(() => {
      expect(result.current.order).toEqual(mockOrder);
    });

    await expect(result.current.cancelOrder()).rejects.toThrow(
      'No pudimos confirmar la cancelación debido a un problema de conexión'
    );

    await waitFor(() => {
      expect(result.current.cancelError).toContain('El pedido continúa en estado confirmado');
    });
  });
});

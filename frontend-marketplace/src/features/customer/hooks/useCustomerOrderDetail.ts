'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback } from 'react';
import { customerOrderService } from '../services/customerOrderService';
import { CUSTOMER_ORDERS_QUERY_KEY } from './useCustomerOrders';
import { CustomerOrderDetail } from '../types/customer.types';

export function useCustomerOrderDetail(orderNumber: string) {
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: ['customer-order', orderNumber],
    queryFn: () => customerOrderService.getOrderDetail(orderNumber),
    enabled: Boolean(orderNumber && orderNumber.trim().length > 0),
    staleTime: 1000 * 60, // 1 minute fresh cache
    gcTime: 1000 * 60 * 5, // 5 minutes retention
    retry: 1,
  });

  const cancelMutation = useMutation({
    mutationFn: (reason?: string) => customerOrderService.cancelOrder(orderNumber, reason),
    onSuccess: (updatedOrder: CustomerOrderDetail) => {
      // Invalidate customer orders list so the tab counters and cards update
      queryClient.invalidateQueries({ queryKey: CUSTOMER_ORDERS_QUERY_KEY });
      // Update cache and refetch current order detail
      queryClient.setQueryData(['customer-order', orderNumber], updatedOrder);
      queryClient.invalidateQueries({ queryKey: ['customer-order', orderNumber] });
    },
  });

  const reload = useCallback(() => {
    query.refetch();
  }, [query]);

  const cancelOrder = useCallback(
    async (reason?: string) => {
      return cancelMutation.mutateAsync(reason);
    },
    [cancelMutation]
  );

  return {
    order: query.data ?? null,
    isLoading: query.isLoading,
    isFetching: query.isFetching,
    error: query.error instanceof Error ? query.error.message : query.error ? 'Error al cargar detalle' : null,
    reload,
    cancelOrder,
    isCancelling: cancelMutation.isPending,
    cancelError:
      cancelMutation.error instanceof Error
        ? cancelMutation.error.message
        : cancelMutation.error
        ? 'Error al cancelar el pedido'
        : null,
    resetCancelError: cancelMutation.reset,
  };
}

'use client';

import { useQuery } from '@tanstack/react-query';
import { useCallback } from 'react';
import { customerOrderService } from '../services/customerOrderService';

export function useCustomerOrderDetail(orderNumber: string) {
  const query = useQuery({
    queryKey: ['customer-order', orderNumber],
    queryFn: () => customerOrderService.getOrderDetail(orderNumber),
    enabled: Boolean(orderNumber && orderNumber.trim().length > 0),
    staleTime: 1000 * 60, // 1 minute fresh cache
    gcTime: 1000 * 60 * 5, // 5 minutes retention
    retry: 1,
  });

  const reload = useCallback(() => {
    query.refetch();
  }, [query]);

  return {
    order: query.data ?? null,
    isLoading: query.isLoading,
    isFetching: query.isFetching,
    error: query.error instanceof Error ? query.error.message : query.error ? 'Error al cargar detalle' : null,
    reload,
  };
}

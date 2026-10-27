'use client';

import { useQuery } from '@tanstack/react-query';
import { useCallback, useMemo } from 'react';
import { customerOrderService } from '../services/customerOrderService';
import { OrderStatus } from '../types/customer.types';

export const CUSTOMER_ORDERS_QUERY_KEY = ['customer-orders'];

const ACTIVE_STATUSES: Set<OrderStatus> = new Set([
  'CONFIRMED',
  'IN_PREPARATION',
  'READY',
  'OUT_FOR_DELIVERY',
]);

export function useCustomerOrders() {
  const query = useQuery({
    queryKey: CUSTOMER_ORDERS_QUERY_KEY,
    queryFn: customerOrderService.getOrders,
    staleTime: 1000 * 60 * 2, // 2 minutes: instant render from cache when navigating back
    gcTime: 1000 * 60 * 10, // 10 minutes cache retention
    retry: 1,
  });

  const orders = useMemo(() => query.data ?? [], [query.data]);

  const activeOrders = useMemo(
    () => orders.filter((o) => ACTIVE_STATUSES.has(o.status)),
    [orders]
  );

  const pastOrders = useMemo(
    () => orders.filter((o) => !ACTIVE_STATUSES.has(o.status)),
    [orders]
  );

  const reload = useCallback(() => {
    query.refetch();
  }, [query]);

  return {
    orders,
    activeOrders,
    pastOrders,
    isLoading: query.isLoading, // true ONLY on first load without cache
    isFetching: query.isFetching,
    error: query.error instanceof Error ? query.error.message : query.error ? 'Error al cargar pedidos' : null,
    reload,
  };
}

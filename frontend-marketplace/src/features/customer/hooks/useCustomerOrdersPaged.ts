'use client';

import { keepPreviousData, useQuery } from '@tanstack/react-query';
import {
  CustomerOrderPageParams,
  customerOrderService,
} from '../services/customerOrderService';

export function useCustomerOrdersPaged(params: CustomerOrderPageParams) {
  const query = useQuery({
    queryKey: ['customer', 'orders', 'paged', params],
    queryFn: () => customerOrderService.getOrdersPaged(params),
    placeholderData: keepPreviousData,
    staleTime: 30_000,
    retry: 1,
  });

  return {
    items: query.data?.items ?? [],
    page: query.data?.page ?? params.page ?? 0,
    size: query.data?.size ?? params.size ?? 6,
    totalElements: query.data?.totalElements ?? 0,
    totalPages: query.data?.totalPages ?? 0,
    isLoading: query.isLoading,
    isFetching: query.isFetching,
    error:
      query.error instanceof Error
        ? query.error.message
        : query.error
        ? 'Error al cargar pedidos'
        : null,
    reload: query.refetch,
  };
}

'use client';

import { keepPreviousData, useQuery } from '@tanstack/react-query';
import {
  CustomerPaymentPageParams,
  getCustomerPaymentHistoryPaged,
} from '../services/customerPaymentService';

export function useCustomerPaymentsPaged(params: CustomerPaymentPageParams) {
  const query = useQuery({
    queryKey: ['customer', 'payments', 'paged', params],
    queryFn: () => getCustomerPaymentHistoryPaged(params),
    placeholderData: keepPreviousData,
    staleTime: 30_000,
    retry: 1,
  });

  return {
    items: query.data?.items ?? [],
    page: query.data?.page ?? params.page ?? 0,
    size: query.data?.size ?? params.size ?? 8,
    totalElements: query.data?.totalElements ?? 0,
    totalPages: query.data?.totalPages ?? 0,
    isLoading: query.isLoading,
    isFetching: query.isFetching,
    isError: query.isError,
    error:
      query.error instanceof Error
        ? query.error.message
        : query.error
        ? 'No pudimos consultar tus pagos.'
        : null,
    refetch: query.refetch,
  };
}
